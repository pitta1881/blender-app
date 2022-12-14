package blender.distributed.Gateway.Servidor;

import blender.distributed.Records.RServidor;
import com.google.gson.Gson;
import io.github.cdimascio.dotenv.Dotenv;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.slf4j.Logger;


import java.io.IOException;
import java.rmi.RemoteException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static blender.distributed.Gateway.GStorage.DeleteObject.deleteObject;
import static blender.distributed.Gateway.GStorage.DownloadObjectIntoMemory.downloadObjectIntoMemory;
import static blender.distributed.Gateway.GStorage.UploadObjectFromMemory.uploadObjectFromMemory;

public class GatewayServidorAction implements IServidorAction {
	Logger log;
	List<RServidor> listaServidores;
	Gson gson = new Gson();
	StatefulRedisConnection redisConnection;
	Dotenv dotenv = Dotenv.load();
	String projectId = dotenv.get("PROJECT_ID");
	String blendBucketName = dotenv.get("BLEND_BUCKET_NAME");
	String partZipBucketName = dotenv.get("PART_ZIP_BUCKET_NAME");
	String finalZipBucketName = dotenv.get("FINAL_ZIP_BUCKET_NAME");

	public GatewayServidorAction(RedisClient redisPrivClient, List<RServidor> listaServidores, Logger log) {
		this.listaServidores = listaServidores;
		this.redisConnection = redisPrivClient.connect();
		this.log = log;
	}
	@Override
	public String helloGatewayFromServidor(String publicIp, int rmiPort) {
		String uuid = UUID.randomUUID().toString();
		RServidor recordServidor = new RServidor(uuid, publicIp, rmiPort, ZonedDateTime.now().toInstant().toEpochMilli());
		String json = gson.toJson(recordServidor);
		redisConnection.sync().hset("listaServidores", uuid ,json);
		log.info("Redis Priv.: hset listaServidores " + uuid + " " + json);
		log.info("Registrado nuevo servidor: " + recordServidor);
		return uuid;
	}

	@Override
	public void helloServerFromWorker(String workerName, String recordWorkerJson) {
		redisConnection.sync().hset("listaWorkers", workerName, recordWorkerJson);
		log.info("Redis Priv.: hset listaWorkers " + workerName + " " + recordWorkerJson);
	}

	@Override
	public void pingAliveFromServidor(String uuidServidor, String publicIp, int rmiPort) {
		RServidor recordServidor = new RServidor(uuidServidor, publicIp, rmiPort, ZonedDateTime.now().toInstant().toEpochMilli());
		String json = gson.toJson(recordServidor);		
		redisConnection.sync().hset("listaServidores", uuidServidor ,json);
		//log.info("Redis Priv.: hset listaServidores " + uuidServidor + " " + recordServidor);	//too much flood
	}
	@Override
	public String getWorker(String workerName) {
		return String.valueOf(redisConnection.sync().hget("listaWorkers", workerName));
	}

	@Override
	public void setWorker(String workerName, String recordWorkerJson) {
		redisConnection.sync().hset("listaWorkers", workerName, recordWorkerJson);
		//log.info("Redis Priv.: hset listaWorkers " + workerName + " " + recordWorkerJson);	 //too much flood
	}

	@Override
	public String getTrabajo(String uuidTrabajo) {
		return String.valueOf(redisConnection.sync().hget("listaTrabajos", uuidTrabajo));
	}
	@Override
	public void setTrabajo(String uuidTrabajo, String recordTrabajoJson) {
		redisConnection.sync().hset("listaTrabajos", uuidTrabajo, recordTrabajoJson);
		log.info("Redis Priv.: hset listaTrabajos " + uuidTrabajo + " " + recordTrabajoJson);
	}
	@Override
	public void delTrabajo(String uuidTrabajo) {
		redisConnection.sync().hdel("listaTrabajos", uuidTrabajo);
		log.info("Redis Priv.: hdel listaTrabajos " + uuidTrabajo);
	}
	@Override
	public String getParte(String uuidParte) {
		return String.valueOf(redisConnection.sync().hget("listaPartes", uuidParte));
	}
	@Override
	public List<String> getAllPartes() {
		return redisConnection.sync().hvals("listaPartes");
	}
	@Override
	public void setParte(String uuidParte, String recordParteJson) {
		redisConnection.sync().hset("listaPartes", uuidParte, recordParteJson);
		log.info("Redis Priv.: hset listaPartes " + uuidParte + " " + recordParteJson);
	}
	@Override
	public void delParte(String uuidParte) {
		redisConnection.sync().hdel("listaPartes", uuidParte);
		log.info("Redis Priv.: hdel listaPartes " + uuidParte);
	}
	@Override
	public void storeBlendFile(String gStorageBlendName, byte[] blendFile) {
		try {
			uploadObjectFromMemory(this.projectId, this.blendBucketName, gStorageBlendName, blendFile);
			log.info("GCS: store to " + this.blendBucketName + " " + gStorageBlendName);
		} catch (IOException e) {
			log.error("Error: " + e.getMessage());
		}
	}
	@Override
	public byte[] getBlendFile(String gStorageBlendName) {
		try {
			byte[] temp = downloadObjectIntoMemory(this.projectId, this.blendBucketName, gStorageBlendName);
			log.info("GCS: download from " + this.blendBucketName + " " + gStorageBlendName);
			return temp;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	@Override
	public void deleteBlendFile(String gStorageBlendName) throws RemoteException {
		try {
			deleteObject(this.projectId, this.blendBucketName, gStorageBlendName);
			log.info("GCS: delete from " + this.blendBucketName + " " + gStorageBlendName);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	@Override
	public void storePartZipFile(String gStorageZipName, byte[] zipFile) {
		try {
			uploadObjectFromMemory(this.projectId, this.partZipBucketName, gStorageZipName, zipFile);
			log.info("GCS: store to " + this.partZipBucketName + " " + gStorageZipName);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	@Override
	public byte[] getPartZipFile(String gStorageZipName) {
		try {
			byte[] temp = downloadObjectIntoMemory(this.projectId, this.partZipBucketName, gStorageZipName);
			log.info("GCS: download from " + this.partZipBucketName + " " + gStorageZipName);
			return temp;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	@Override
	public void deletePartZipFile(String gStorageZipName) throws RemoteException {
		try {
			deleteObject(this.projectId, this.partZipBucketName, gStorageZipName);
			log.info("GCS: delete from " + this.partZipBucketName + " " + gStorageZipName);
		} catch (IOException e) {
			log.error("Error: " + e.getMessage());
		}
	}
	@Override
	public void storeFinalZipFile(String gStorageZipName, byte[] zipFile) {
		try {
			uploadObjectFromMemory(this.projectId, this.finalZipBucketName, gStorageZipName, zipFile);
			log.info("GCS: store to " + this.finalZipBucketName + " " + gStorageZipName);
		} catch (IOException e) {
			log.error("Error: " + e.getMessage());
		}
	}

}

