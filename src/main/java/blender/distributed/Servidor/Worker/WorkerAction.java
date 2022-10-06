package blender.distributed.Servidor.Worker;

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
import java.rmi.RemoteException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Map;

public class WorkerAction implements IWorkerAction{
	Logger log = LoggerFactory.getLogger(WorkerAction.class);
	Map<String,LocalTime> workersLastPing;
	Map<String, PairTrabajoParte> listaWorkers;
	ArrayList<Trabajo> listaTrabajos;
	String serverDirectory;

	public WorkerAction(Map<String, PairTrabajoParte> listaWorkers, ArrayList<Trabajo> listaTrabajos, Map<String, LocalTime> workersLastPing, String serverDirectory) {
		MDC.put("log.name", WorkerAction.class.getSimpleName());
		this.listaWorkers = listaWorkers;
		this.listaTrabajos = listaTrabajos;
		this.workersLastPing = workersLastPing;
		this.serverDirectory = serverDirectory;
	}

	@Override
	public void pingAlive(String workerName) throws RemoteException {
		synchronized (this.listaTrabajos) {
			if(!this.listaWorkers.containsKey(workerName)) {
				this.listaWorkers.put(workerName, null);
				log.info("Registrando nuevo worker: "+workerName);
			}
		}
		synchronized (workersLastPing) {
			workersLastPing.put(workerName,LocalTime.now());
		}
	}

	@Override
	public PairTrabajoParte giveWorkToDo(String workerName) throws RemoteException {
		synchronized (listaTrabajos) {
			if (listaTrabajos.size() == 0) {
				return null;
			}
			Trabajo work = listaTrabajos.stream().filter(trabajo -> trabajo.getStatus() == TrabajoStatus.TO_DO).findFirst().orElse(null);
			if (work == null) {
				return null;
			}
			TrabajoPart part = work.getListaPartes().stream().filter(parte -> parte.getStatus() == TrabajoStatus.TO_DO).findFirst().orElse(null);
			if(part == null){
				return null;
			}
			part.setStatus(TrabajoStatus.IN_PROGRESS);
			PairTrabajoParte ptp = new PairTrabajoParte(work, part);
			synchronized (listaWorkers){
				this.listaWorkers.put(workerName, ptp);
			}
			return ptp;
		}
	}

	@Override
	public void setTrabajoParteStatusDone(String workerName, String trabajoId, int nParte, byte[] zipWithRenderedImages) throws RemoteException {
		synchronized (listaWorkers){
			this.listaWorkers.put(workerName, null);
		}
		synchronized (listaTrabajos) {
			Trabajo work = listaTrabajos.stream().filter(trabajo -> trabajoId.equals(trabajo.getId())).findFirst().orElse(null);
			if (work != null) {
				TrabajoPart parte = work.getParte(nParte);
				parte.setStatus(TrabajoStatus.DONE);
				parte.setZipWithRenderedImages(zipWithRenderedImages);
				if(work.getStatus() == TrabajoStatus.DONE){
					String workDir = this.serverDirectory + "\\Works\\";
					String thisWorkDir = this.serverDirectory + "\\Works\\" + work.getBlendName() + "\\";
					DirectoryTools.checkOrCreateFolder(workDir);
					DirectoryTools.checkOrCreateFolder(thisWorkDir);

					for (TrabajoPart part: work.getListaPartes()) {
						String zipPartPath = thisWorkDir + work.getBlendName()+"__part__"+parte.getNParte()+".zip";
						File zipPartFile = new File(zipPartPath);
						try (FileOutputStream fos = new FileOutputStream(zipPartFile)) {
							fos.write(part.getZipWithRenderedImages());
							new ZipFile(zipPartPath).extractAll(thisWorkDir);
						} catch (Exception e) {
							log.error("ERROR: " + e.getMessage());
						}
					}
					try {
						new ZipFile(thisWorkDir + work.getBlendName()+".zip").addFolder(new File(thisWorkDir + "\\RenderedFiles\\"));
						File zipResult = new File(thisWorkDir + work.getBlendName()+".zip");
						byte[] finalZipWithRenderedImages = Files.readAllBytes(zipResult.toPath());
						work.setZipWithRenderedImages(finalZipWithRenderedImages);
						File workDirFile = new File(thisWorkDir);
						DirectoryTools.deleteDirectory(workDirFile);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
	}
}
