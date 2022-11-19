package blender.distributed.Servidor.Worker;

import blender.distributed.Gateway.PairParteLastping;
import blender.distributed.Gateway.Servidor.IGatewayServidorAction;
import blender.distributed.Servidor.Trabajo.PairTrabajoParte;
import blender.distributed.Servidor.Trabajo.Trabajo;
import blender.distributed.Servidor.Trabajo.TrabajoPart;
import blender.distributed.Servidor.Trabajo.TrabajoStatus;
import blender.distributed.SharedTools.DirectoryTools;
import net.lingala.zip4j.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalTime;
import java.util.List;

public class WorkerAction implements IWorkerAction{
	Logger log = LoggerFactory.getLogger(WorkerAction.class);
	String singleServerDir;
	IGatewayServidorAction stubGateway;
	String gatewayIp;
	int gatewayPort;


	public WorkerAction(String gatewayIp, int gatewayPort, String singleServerDir) {
		MDC.put("log.name", WorkerAction.class.getSimpleName());
		this.singleServerDir = singleServerDir;
		this.gatewayIp = gatewayIp;
		this.gatewayPort = gatewayPort;
	}

	@Override
	public void pingAlive(String workerName) {
		try {
			synchronized (this.stubGateway) {
				PairParteLastping workerRecord = this.stubGateway.getWorker(workerName);
				if (workerRecord == null) {
					log.info("Registrando nuevo worker: " + workerName);
					workerRecord = new PairParteLastping(null, LocalTime.now());
				} else {
					workerRecord = new PairParteLastping(workerRecord.ptp(), LocalTime.now());
				}
				this.stubGateway.setWorker(workerName, workerRecord);
			}
		} catch (RemoteException | NullPointerException e) {
			connectRMI();
			pingAlive(workerName);
		}
	}


	@Override
	public PairTrabajoParte giveWorkToDo(String workerName) {
		try {
			synchronized (this.stubGateway) {
					List<Object> tr = this.stubGateway.getAllTrabajos();
					if (tr.size() == 0) {
						return null;
					}
					List<Trabajo> trabajos = (List<Trabajo>) (Object) tr;
					Trabajo work = trabajos.stream().filter(trabajo -> trabajo.getStatus() == TrabajoStatus.TO_DO).findFirst().orElse(null);
					if (work == null) {
						return null;
					}
					TrabajoPart part = work.getListaPartes().stream().filter(parte -> parte.getStatus() == TrabajoStatus.TO_DO).findFirst().orElse(null);
					if (part == null) {
						return null;
					}
					work.getParte(part.getNParte()).setStatus(TrabajoStatus.IN_PROGRESS);
					this.stubGateway.setTrabajo(work);
					PairTrabajoParte ptp = new PairTrabajoParte(work, work.getParte(part.getNParte()));
					this.stubGateway.setWorker(workerName, new PairParteLastping(ptp, LocalTime.now()));

					return ptp;
			}
		} catch (RemoteException | NullPointerException e) {
			connectRMI();
			return giveWorkToDo(workerName);
		}
	}

	@Override
	public void setTrabajoParteStatusDone(String workerName, String trabajoId, int nParte, byte[] zipWithRenderedImages) {
		try {
			synchronized (this.stubGateway) {
				this.stubGateway.setWorker(workerName, new PairParteLastping(null, LocalTime.now()));
				Trabajo work = this.stubGateway.getTrabajo(trabajoId);
				if (work != null) {
					TrabajoPart part = work.getParte(nParte);
					work.getParte(part.getNParte()).setStatus(TrabajoStatus.DONE);
					work.getParte(part.getNParte()).setZipWithRenderedImages(zipWithRenderedImages);
					this.stubGateway.setTrabajo(work);
					if (work.getStatus() == TrabajoStatus.DONE) {
						Trabajo workFinal = this.stubGateway.getTrabajo(trabajoId);
						String workDir = this.singleServerDir + "\\Works\\";
						String thisWorkDir = this.singleServerDir + "\\Works\\" + workFinal.getBlendName() + "\\";
						DirectoryTools.checkOrCreateFolder(workDir);
						DirectoryTools.checkOrCreateFolder(thisWorkDir);

						for (TrabajoPart parte : workFinal.getListaPartes()) {
							String zipPartPath = thisWorkDir + workFinal.getBlendName() + "__part__" + parte.getNParte() + ".zip";
							File zipPartFile = new File(zipPartPath);
							try (FileOutputStream fos = new FileOutputStream(zipPartFile)) {
								fos.write(part.getZipWithRenderedImages());
								new ZipFile(zipPartPath).extractAll(thisWorkDir);
							} catch (Exception e) {
								log.error("ERROR: " + e.getMessage());
							}
						}
						try {
							new ZipFile(thisWorkDir + workFinal.getBlendName() + ".zip").addFolder(new File(thisWorkDir + "\\RenderedFiles\\"));
							File zipResult = new File(thisWorkDir + workFinal.getBlendName() + ".zip");
							byte[] finalZipWithRenderedImages = Files.readAllBytes(zipResult.toPath());
							workFinal.setZipWithRenderedImages(finalZipWithRenderedImages);
							this.stubGateway.setTrabajo(workFinal);
							File workDirFile = new File(thisWorkDir);
							DirectoryTools.deleteDirectory(workDirFile);
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
				}
			}
		} catch (RemoteException | NullPointerException e) {
			connectRMI();
			setTrabajoParteStatusDone(workerName, trabajoId, nParte, zipWithRenderedImages);
		}
	}

	private void connectRMI() {
		this.stubGateway = null;
		try {
			Thread.sleep(1000);
			Registry workerRMI = LocateRegistry.getRegistry(this.gatewayIp, this.gatewayPort);
			this.stubGateway = (IGatewayServidorAction) workerRMI.lookup("servidorAction");
			log.info("Conectado al Gateway " + this.gatewayIp + ":" + this.gatewayPort);
		} catch (RemoteException | NotBoundException | InterruptedException e) {
			log.error("Error al conectar con el Gateway " + this.gatewayIp + ":" + this.gatewayPort);
			connectRMI();
		}
	}
}
