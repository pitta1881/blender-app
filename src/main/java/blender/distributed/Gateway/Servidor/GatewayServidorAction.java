package blender.distributed.Gateway.Servidor;

import blender.distributed.Records.RServidor;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.Type;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static java.rmi.server.RemoteServer.getClientHost;

public class GatewayServidorAction implements IGatewayServidorAction {
	Logger log = LoggerFactory.getLogger(GatewayServidorAction.class);
	List<RServidor> listaServidores;
	Gson gson = new Gson();
	StatefulRedisConnection redisConnection;

	public GatewayServidorAction(RedisClient redisPrivClient, List<RServidor> listaServidores) {
		MDC.put("log.name", GatewayServidorAction.class.getSimpleName());
		this.listaServidores = listaServidores;
		redisConnection = redisPrivClient.connect();
	}
	@Override
	public String helloGatewayFromServidor(int rmiPortForClientes, int rmiPortForWorkers) {
		String uuid = UUID.randomUUID().toString();
		RServidor recordServidor = null;
		try {
			recordServidor = new RServidor(uuid, getClientHost(), rmiPortForClientes, rmiPortForWorkers, LocalTime.now().toString());
		} catch (ServerNotActiveException e) {
			throw new RuntimeException(e);
		}		
		redisConnection.sync().hset("listaServidores", uuid ,gson.toJson(recordServidor));
		log.info("Registrado nuevo servidor: " + recordServidor);
		return uuid;
	}
	@Override
	public void pingAliveFromServidor(String uuidServidor, int rmiPortForClientes, int rmiPortForWorkers) {
		RServidor recordServidor = null;
		try {
			recordServidor = new RServidor(uuidServidor, getClientHost(),rmiPortForClientes, rmiPortForWorkers, LocalTime.now().toString());
		} catch (ServerNotActiveException e) {
			throw new RuntimeException(e);
		}
		String json = gson.toJson(recordServidor);		
		redisConnection.sync().hset("listaServidores", uuidServidor ,json);
	}
	@Override
	public String getWorker(String workerName) {		
		String workerRecordJson = String.valueOf(redisConnection.sync().hget("listaWorkers", workerName));
		return workerRecordJson;
	}
	@Override
	public String getAllWorkers() throws RemoteException {
		String listaWorkersJson = String.valueOf(redisConnection.sync().hvals("listaWorkers"));
		return listaWorkersJson;
	}

	@Override
	public void setWorker(String workerName, String recordWorkerJson) throws RemoteException {
		redisConnection.sync().hset("listaWorkers", workerName, recordWorkerJson);
	}

	@Override
	public void delWorker(String workerName) {
		redisConnection.sync().hdel("listaWorkers", workerName);
	}
	@Override
	public String getTrabajo(String uuidTrabajo) {
		String trabajoRecordJson = String.valueOf(redisConnection.sync().hget("listaTrabajos", uuidTrabajo));
		return trabajoRecordJson;
	}
	@Override
	public String getAllTrabajos() {
		String listaTrabajosJson = String.valueOf(redisConnection.sync().hvals("listaTrabajos"));
		return listaTrabajosJson;
	}
	@Override
	public void setTrabajo(String uuidTrabajo, String recordTrabajoJson) {
		redisConnection.sync().hset("listaTrabajos", uuidTrabajo, recordTrabajoJson);
	}
	@Override
	public void delTrabajo(String uuidTrabajo) {
		redisConnection.sync().hdel("listaTrabajos", uuidTrabajo);
	}
	@Override
	public String getParte(String uuidParte) {
		String parteRecordJson = String.valueOf(redisConnection.sync().hget("listaPartes", uuidParte));
		return parteRecordJson;
	}
	@Override
	public List<String> getAllPartes() {
		List<String> listaPartesJson = redisConnection.sync().hvals("listaPartes");
		return listaPartesJson;
	}
	@Override
	public void setParte(String uuidParte, String recordParteJson) {
		redisConnection.sync().hset("listaPartes", uuidParte, recordParteJson);
	}
	@Override
	public void delParte(String uuidParte) {
		redisConnection.sync().hdel("listaPartes", uuidParte);
	}
	@Override
	public String storeBlendFile(String blendName, byte[] blendFile) {
		//TODO: store somewhere this blendFile
		return "https://example.cloud.com/"+blendName+".blend";
	}

	@Override
	public String storeZipFile(String zipName, byte[] zipFile) throws RemoteException {
		//TODO: store somewhere this zipFile
		return "https://example.cloud.com/"+zipName+".zip";
	}

	@Override
	public byte[] getZipFile(String uuidParte) {
		//TODO: get stored zip file uuidParte.zip
		return new byte[0];
	}

}

