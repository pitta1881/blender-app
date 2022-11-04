package blender.distributed.Servidor.Worker;

import blender.distributed.Gateway.PairParteLastping;
import blender.distributed.Servidor.SerializedObjectCodec;
import blender.distributed.Servidor.Trabajo.PairTrabajoParte;
import blender.distributed.Servidor.Trabajo.Trabajo;
import blender.distributed.Servidor.Trabajo.TrabajoPart;
import blender.distributed.Servidor.Trabajo.TrabajoStatus;
import blender.distributed.SharedTools.DirectoryTools;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
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
	Map<String, PairTrabajoParte> listaWorkers;
	ArrayList<Trabajo> listaTrabajos;
	String singleServerDir;
	RedisClient redisClient;


	public WorkerAction(RedisClient redisClient, String singleServerDir) {
		MDC.put("log.name", WorkerAction.class.getSimpleName());
		this.singleServerDir = singleServerDir;
		this.redisClient = redisClient;
	}

	@Override
	public void pingAlive(String workerName){
		StatefulRedisConnection<String, Object> redisConnection = this.redisClient.connect(new SerializedObjectCodec());
		RedisCommands<String, Object> syncCommands = redisConnection.sync();

		PairParteLastping workerRecord = (PairParteLastping) syncCommands.hget("listaWorkers", workerName);
		if(workerRecord == null) {
			log.info("Registrando nuevo worker: " + workerName);
			workerRecord = new PairParteLastping(null, LocalTime.now());
		} else {
			workerRecord = new PairParteLastping(workerRecord.ptp(), LocalTime.now());
		}
		syncCommands.hset("listaWorkers", workerName, workerRecord);

		redisConnection.close();
	}


	@Override
	public PairTrabajoParte giveWorkToDo(String workerName) throws RemoteException {
		/*
		try (Jedis jedis = this.pool.getResource()) {
			Map<byte[], byte[]> listaTrabajos = jedis.hgetAll(this.listaTrabajosByte);
			if (listaTrabajos.size() == 0) {
				return null;
			}
			ArrayList<Trabajo> works = SerializationUtils.deserialize((InputStream) listaTrabajos.values());
			Trabajo work = works.stream().filter(trabajo -> trabajo.getStatus() == TrabajoStatus.TO_DO).findFirst().orElse(null);
			if (work == null) {
				return null;
			}
			TrabajoPart part = work.getListaPartes().stream().filter(parte -> parte.getStatus() == TrabajoStatus.TO_DO).findFirst().orElse(null);
			if (part == null) {
				return null;
			}
			part.setStatus(TrabajoStatus.IN_PROGRESS);
			PairTrabajoParte ptp = new PairTrabajoParte(work, part);
			byte[] workerNameByte = SerializationUtils.serialize(workerName);
			PairParteLastping workerRecord = new PairParteLastping(ptp, LocalTime.now());
			jedis.hset(this.listaWorkersByte, workerNameByte, SerializationUtils.serialize(workerRecord));
			jedis.close();

			return ptp;
		 */
			return null;
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
					String workDir = this.singleServerDir + "\\Works\\";
					String thisWorkDir = this.singleServerDir + "\\Works\\" + work.getBlendName() + "\\";
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
