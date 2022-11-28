package blender.distributed.Servidor.Worker;

import blender.distributed.Enums.EStatus;
import blender.distributed.Records.*;
import blender.distributed.Servidor.Cliente.ClienteAction;
import blender.distributed.SharedTools.DirectoryTools;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.lingala.zip4j.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static blender.distributed.Gateway.Tools.connectRandomGatewayRMIForServidor;

public class WorkerAction implements IWorkerAction{
	Logger log = LoggerFactory.getLogger(WorkerAction.class);
	String singleServerDir;
	List<RGateway> listaGateways;
	Gson gson = new Gson();
	Type RTrabajoType = new TypeToken<RTrabajo>(){}.getType();
	Type RWorkerType = new TypeToken<RWorker>(){}.getType();
	Type RParteType = new TypeToken<RParte>(){}.getType();
	public WorkerAction(List<RGateway> listaGateways, String singleServerDir) {
		MDC.put("log.name", ClienteAction.class.getSimpleName());
		this.singleServerDir = singleServerDir;
		this.listaGateways = listaGateways;
	}

	@Override
	public void pingAlive(String workerName) {
		try {
			String stringRecordWorker = connectRandomGatewayRMIForServidor(this.listaGateways).getWorker(workerName);
			RWorker workerRecord = gson.fromJson(stringRecordWorker, RWorkerType);
			if (workerRecord == null) {
				log.info("Registrando nuevo worker: " + workerName);
				workerRecord = new RWorker(workerName, null, LocalTime.now().toString());
			} else {
				workerRecord = new RWorker(workerName, workerRecord.uuidParte(), LocalTime.now().toString());
			}
			connectRandomGatewayRMIForServidor(this.listaGateways).setWorker(workerName, gson.toJson(workerRecord));
		} catch (RemoteException | NullPointerException e) {
			pingAlive(workerName);
		}
	}


	@Override
	public String getWorkToDo(String workerName) {
		try {
			List<String> listaPartesJson = connectRandomGatewayRMIForServidor(this.listaGateways).getAllPartes();
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
			connectRandomGatewayRMIForServidor(this.listaGateways).setParte(recordParte.uuidTrabajo(), gson.toJson(recordParteUpdated));
			RWorker recordWorkerUpdated = new RWorker(workerName, recordParte.uuid(), LocalTime.now().toString());
			connectRandomGatewayRMIForServidor(this.listaGateways).setWorker(workerName, gson.toJson(recordWorkerUpdated));

			String recordTrabajoJson = connectRandomGatewayRMIForServidor(this.listaGateways).getTrabajo(recordParte.uuidTrabajo());
			RTrabajo recordTrabajo = gson.fromJson(recordTrabajoJson, RTrabajoType);
			return gson.toJson(new RTrabajoParte(recordTrabajo, recordParteUpdated));
		} catch (RemoteException | NullPointerException e) {
			return getWorkToDo(workerName);
		}
	}

	@Override
	public void setParteDone(String workerName, String uuidParte, byte[] zipParteWithRenderedImages) {
		try {
			connectRandomGatewayRMIForServidor(this.listaGateways).setWorker(workerName, gson.toJson(new RWorker(workerName, null, LocalTime.now().toString())));
			String recordParteJson = connectRandomGatewayRMIForServidor(this.listaGateways).getParte(uuidParte);
			RParte recordParte = gson.fromJson(recordParteJson, RParteType);
			if (recordParte != null) {
				String urlZipParte = connectRandomGatewayRMIForServidor(this.listaGateways).storeZipFile(uuidParte, zipParteWithRenderedImages);
				RParte recordParteUpdated = new RParte(recordParte.uuidTrabajo(), recordParte.uuid(),recordParte.startFrame(), recordParte.endFrame(), EStatus.DONE, urlZipParte);
				connectRandomGatewayRMIForServidor(this.listaGateways).setParte(recordParte.uuid(), gson.toJson(recordParteUpdated));

				String recordTrabajoJson = connectRandomGatewayRMIForServidor(this.listaGateways).getTrabajo(recordParte.uuidTrabajo());
				RTrabajo recordTrabajo = gson.fromJson(recordTrabajoJson, RTrabajoType);
				boolean trabajoTerminado = true;
				int i = 0;
				while (trabajoTerminado || i <= recordTrabajo.listaPartes().size()){
					String parteTempJson = connectRandomGatewayRMIForServidor(this.listaGateways).getParte(recordTrabajo.listaPartes().get(i));
					RParte parteTemp = gson.fromJson(parteTempJson, RParteType);
					if(parteTemp.estado() == EStatus.TO_DO || parteTemp.estado() == EStatus.IN_PROGRESS) {
						trabajoTerminado = false;
					}
					i++;
				}
				if (trabajoTerminado) {

					String workDir = this.singleServerDir + "/Works/";
					String thisWorkDir = this.singleServerDir + "/Works/" + recordTrabajo.blendName() + "/";
					DirectoryTools.checkOrCreateFolder(workDir);
					DirectoryTools.checkOrCreateFolder(thisWorkDir);

					recordTrabajo.listaPartes().forEach(parte -> {
						String zipPartPath = thisWorkDir + recordTrabajo.blendName() + "__part__" + parte + ".zip";
						File zipPartFile = new File(zipPartPath);
						try (FileOutputStream fos = new FileOutputStream(zipPartFile)) {
							byte[] bytesZipParte = connectRandomGatewayRMIForServidor(this.listaGateways).getZipFile(parte);
							fos.write(bytesZipParte);
							new ZipFile(zipPartPath).extractAll(thisWorkDir);
						} catch (Exception e) {
							log.error("ERROR: " + e.getMessage());
						}
					});

					try {
						new ZipFile(thisWorkDir + recordTrabajo.blendName() + ".zip").addFolder(new File(thisWorkDir + "/RenderedFiles/"));
						File zipResult = new File(thisWorkDir + recordTrabajo.blendName() + ".zip");
						byte[] zipTrabajoWithRenderedImages = Files.readAllBytes(zipResult.toPath());
						File workDirFile = new File(thisWorkDir);
						DirectoryTools.deleteDirectory(workDirFile);

						String urlZipFinal = connectRandomGatewayRMIForServidor(this.listaGateways).storeZipFile(uuidParte, zipTrabajoWithRenderedImages);
						connectRandomGatewayRMIForServidor(this.listaGateways).setTrabajo(recordTrabajo.uuid(), gson.toJson(new RTrabajo(recordTrabajo.uuid(), recordTrabajo.blendName(), recordTrabajo.startFrame(), recordTrabajo.endFrame(), EStatus.DONE, recordTrabajo.listaPartes(), urlZipFinal)));
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		} catch (RemoteException | NullPointerException e) {
			setParteDone(workerName, uuidParte, zipParteWithRenderedImages);
		}
	}
}

