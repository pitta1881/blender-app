package blender.distributed.Servidor.Worker;

import blender.distributed.Enums.ENodo;
import blender.distributed.Enums.EStatus;
import blender.distributed.Records.*;
import blender.distributed.SharedTools.DirectoryTools;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.lingala.zip4j.ZipFile;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static blender.distributed.Servidor.Tools.connectRandomGatewayRMIForServidor;

public class WorkerAction implements IWorkerAction{
	Logger log;
	String singleServerDir;
	List<RGateway> listaGateways;
	Gson gson = new Gson();
	Type RTrabajoType = new TypeToken<RTrabajo>(){}.getType();
	Type RWorkerType = new TypeToken<RWorker>(){}.getType();
	Type RParteType = new TypeToken<RParte>(){}.getType();
	public WorkerAction(List<RGateway> listaGateways, String singleServerDir, Logger log) {
		MDC.put("log.name", ENodo.SERVIDOR.name());
		this.singleServerDir = singleServerDir;
		this.listaGateways = listaGateways;
		this.log = log;
	}

	@Override
	public void pingAlive(String workerName) {
		try {
			String stringRecordWorker = connectRandomGatewayRMIForServidor(this.listaGateways, this.log).getWorker(workerName);
			RWorker workerRecord = gson.fromJson(stringRecordWorker, RWorkerType);
			if (workerRecord == null) {
				log.info("Registrando nuevo worker: " + workerName);
				workerRecord = new RWorker(workerName, null, ZonedDateTime.now().toInstant().toEpochMilli());
			} else {
				workerRecord = new RWorker(workerName, workerRecord.uuidParte(), ZonedDateTime.now().toInstant().toEpochMilli());
			}
			connectRandomGatewayRMIForServidor(this.listaGateways, this.log).setWorker(workerName, gson.toJson(workerRecord));
		} catch (RemoteException | NullPointerException e) {
			pingAlive(workerName);
		}
	}


	@Override
	public String getWorkToDo(String workerName) {
		try {
			List<String> listaPartesJson = connectRandomGatewayRMIForServidor(this.listaGateways, this.log).getAllPartes();
			if (listaPartesJson.size() == 0) {
				return null;
			}
			List<RParte> listaPartes = new ArrayList<>();
			listaPartesJson.forEach(lpJson -> {
				listaPartes.add(gson.fromJson(lpJson, RParteType));
			});
			RParte recordParte = listaPartes.stream().filter(parte -> parte.estado() == EStatus.TO_DO).findFirst().orElse(null);
			if (recordParte == null) {
				return null;
			}
			RParte recordParteUpdated = new RParte(recordParte.uuidTrabajo(), recordParte.uuid(),recordParte.startFrame(), recordParte.endFrame(), EStatus.IN_PROGRESS, null);
			connectRandomGatewayRMIForServidor(this.listaGateways, this.log).setParte(recordParte.uuid(), gson.toJson(recordParteUpdated));
			RWorker recordWorkerUpdated = new RWorker(workerName, recordParte.uuid(), ZonedDateTime.now().toInstant().toEpochMilli());
			connectRandomGatewayRMIForServidor(this.listaGateways, this.log).setWorker(workerName, gson.toJson(recordWorkerUpdated));

			String recordTrabajoJson = connectRandomGatewayRMIForServidor(this.listaGateways, this.log).getTrabajo(recordParte.uuidTrabajo());
			RTrabajo recordTrabajo = gson.fromJson(recordTrabajoJson, RTrabajoType);
			return gson.toJson(new RTrabajoParte(recordTrabajo, recordParteUpdated));
		} catch (RemoteException | NullPointerException e) {
			return getWorkToDo(workerName);
		}
	}

	@Override
	public void setParteDone(String workerName, String uuidParte, byte[] zipParteWithRenderedImages) {
		try {
			connectRandomGatewayRMIForServidor(this.listaGateways, this.log).setWorker(workerName, gson.toJson(new RWorker(workerName, null, ZonedDateTime.now().toInstant().toEpochMilli())));
			String recordParteJson = connectRandomGatewayRMIForServidor(this.listaGateways, this.log).getParte(uuidParte);
			RParte recordParte = gson.fromJson(recordParteJson, RParteType);
			if (recordParte != null) {
				connectRandomGatewayRMIForServidor(this.listaGateways, this.log).storePartZipFile(uuidParte+".zip", zipParteWithRenderedImages);
				RParte recordParteUpdated = new RParte(recordParte.uuidTrabajo(), recordParte.uuid(),recordParte.startFrame(), recordParte.endFrame(), EStatus.DONE, uuidParte+".zip");
				connectRandomGatewayRMIForServidor(this.listaGateways, this.log).setParte(recordParte.uuid(), gson.toJson(recordParteUpdated));

				String recordTrabajoJson = connectRandomGatewayRMIForServidor(this.listaGateways, this.log).getTrabajo(recordParte.uuidTrabajo());
				RTrabajo recordTrabajo = gson.fromJson(recordTrabajoJson, RTrabajoType);
				boolean trabajoTerminado = true;
				int i = 0;
				while (trabajoTerminado && i < recordTrabajo.listaPartes().size()){
					String parteTempJson = connectRandomGatewayRMIForServidor(this.listaGateways, this.log).getParte(recordTrabajo.listaPartes().get(i));
					RParte parteTemp = gson.fromJson(parteTempJson, RParteType);
					if(parteTemp.estado() == EStatus.TO_DO || parteTemp.estado() == EStatus.IN_PROGRESS) {
						trabajoTerminado = false;
					}
					i++;
				}
				if (trabajoTerminado) {
					String workDir = this.singleServerDir + "/Works/";
					String thisWorkDir = this.singleServerDir + "/Works/" + recordTrabajo.uuid() + "/";
					DirectoryTools.checkOrCreateFolder(workDir, ENodo.SERVIDOR.name());
					DirectoryTools.checkOrCreateFolder(thisWorkDir, ENodo.SERVIDOR.name());

					byte[] zipTrabajoWithRenderedImages = new byte[0];
					if(recordTrabajo.listaPartes().size() == 1){
						zipTrabajoWithRenderedImages = zipParteWithRenderedImages;
					} else {
						recordTrabajo.listaPartes().forEach(parte -> {
							String zipPartPath = thisWorkDir + recordTrabajo.uuid() + "__part__" + parte + ".zip";
							File zipPartFile = new File(zipPartPath);
							try (FileOutputStream fos = new FileOutputStream(zipPartFile)) {
								byte[] bytesZipParte = connectRandomGatewayRMIForServidor(this.listaGateways, this.log).getPartZipFile(parte + ".zip");
								fos.write(bytesZipParte);
								new ZipFile(zipPartPath).extractAll(thisWorkDir + "/RenderedFiles/");
							} catch (Exception e) {
								log.error("ERROR: " + e.getMessage());
							}
						});

						try {
							new ZipFile(thisWorkDir + recordTrabajo.uuid() + ".zip").addFolder(new File(thisWorkDir + "/RenderedFiles/"));
							File zipResult = new File(thisWorkDir + recordTrabajo.uuid() + ".zip");
							zipTrabajoWithRenderedImages = Files.readAllBytes(zipResult.toPath());
						} catch (IOException e) {
							log.error("Error: " + e.getMessage());
						}
					}
					connectRandomGatewayRMIForServidor(this.listaGateways, this.log).storeFinalZipFile(recordTrabajo.uuid()+".zip", zipTrabajoWithRenderedImages);
					connectRandomGatewayRMIForServidor(this.listaGateways, this.log).setTrabajo(recordTrabajo.uuid(), gson.toJson(new RTrabajo(recordTrabajo.uuid(), recordTrabajo.blendName(), recordTrabajo.startFrame(), recordTrabajo.endFrame(), EStatus.DONE, recordTrabajo.listaPartes(), recordTrabajo.gStorageBlendName(), recordTrabajo.uuid()+".zip", recordTrabajo.createdAt())));
					connectRandomGatewayRMIForServidor(this.listaGateways, this.log).deleteBlendFile(recordTrabajo.gStorageBlendName());
					recordTrabajo.listaPartes().forEach(parte -> {
						try {
							connectRandomGatewayRMIForServidor(this.listaGateways, this.log).deletePartZipFile(parte+".zip");
							connectRandomGatewayRMIForServidor(this.listaGateways, this.log).delParte(parte);
						} catch (RemoteException e) {
							log.error("Error: " + e.getMessage());
						}
					});
					File workDirFile = new File(thisWorkDir);
					DirectoryTools.deleteDirectory(workDirFile);
				}
			}
		} catch (RemoteException | NullPointerException e) {
			setParteDone(workerName, uuidParte, zipParteWithRenderedImages);
		}
	}

	@Override
	public byte[] getBlendFile(String gStorageBlendName) {
		try {
			return connectRandomGatewayRMIForServidor(this.listaGateways, this.log).getBlendFile(gStorageBlendName);
		} catch (RemoteException e) {
			return getBlendFile(gStorageBlendName);
		}
	}
}

