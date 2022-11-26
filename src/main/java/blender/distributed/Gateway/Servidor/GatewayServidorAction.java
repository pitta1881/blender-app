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
	RedisClient redisPrivClient;
	List<RServidor> listaServidores;
	Gson gson = new Gson();
	Type RServidorType = new TypeToken<RServidor>(){}.getType();

	public GatewayServidorAction(RedisClient redisPrivClient, List<RServidor> listaServidores) {
		MDC.put("log.name", GatewayServidorAction.class.getSimpleName());
		this.redisPrivClient = redisPrivClient;
		this.listaServidores = listaServidores;
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
		StatefulRedisConnection redisConnection = this.redisPrivClient.connect();
		redisConnection.sync().hset("listaServidores", uuid ,gson.toJson(recordServidor));
		log.info("Registrado nuevo servidor: " + recordServidor);
		redisConnection.close();
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
		StatefulRedisConnection redisConnection = this.redisPrivClient.connect();
		redisConnection.sync().hset("listaServidores", uuidServidor ,json);
		redisConnection.close();
	}
	@Override
	public String getWorker(String workerName) {
		StatefulRedisConnection redisConnection = this.redisPrivClient.connect();
		String workerRecordJson = String.valueOf(redisConnection.sync().hget("listaWorkers", workerName));
		redisConnection.close();
		return workerRecordJson;
	}
	@Override
	public String getAllWorkers() throws RemoteException {
		StatefulRedisConnection redisConnection = this.redisPrivClient.connect();
		String listaWorkersJson = String.valueOf(redisConnection.sync().hvals("listaWorkers"));
		redisConnection.close();
		return listaWorkersJson;
	}

	@Override
	public void setWorker(String workerName, String recordWorkerJson) throws RemoteException {
		StatefulRedisConnection redisConnection = this.redisPrivClient.connect();
		redisConnection.sync().hset("listaWorkers", workerName, recordWorkerJson);
		redisConnection.close();
	}

	@Override
	public void delWorker(String workerName) {
		StatefulRedisConnection redisConnection = this.redisPrivClient.connect();
		redisConnection.sync().hdel("listaWorkers", workerName);
		redisConnection.close();
	}
	@Override
	public String getTrabajo(String uuidTrabajo) {
		StatefulRedisConnection redisConnection = this.redisPrivClient.connect();
		String trabajoRecordJson = String.valueOf(redisConnection.sync().hget("listaTrabajos", uuidTrabajo));
		redisConnection.close();
		return trabajoRecordJson;
	}
	@Override
	public String getAllTrabajos() {
		StatefulRedisConnection redisConnection = this.redisPrivClient.connect();
		String listaTrabajosJson = String.valueOf(redisConnection.sync().hvals("listaTrabajos"));
		redisConnection.close();
		return listaTrabajosJson;
	}
	@Override
	public void setTrabajo(String uuidTrabajo, String recordTrabajoJson) {
		StatefulRedisConnection redisConnection = this.redisPrivClient.connect();
		redisConnection.sync().hset("listaTrabajos", uuidTrabajo, recordTrabajoJson);
		redisConnection.close();
	}
	@Override
	public void delTrabajo(String uuidTrabajo) {
		StatefulRedisConnection redisConnection = this.redisPrivClient.connect();
		redisConnection.sync().hdel("listaTrabajos", uuidTrabajo);
		redisConnection.close();
	}
	@Override
	public String getParte(String uuidParte) {
		StatefulRedisConnection redisConnection = this.redisPrivClient.connect();
		String parteRecordJson = String.valueOf(redisConnection.sync().hget("listaPartes", uuidParte));
		redisConnection.close();
		return parteRecordJson;
	}
	@Override
	public List<String> getAllPartes() {
		StatefulRedisConnection redisConnection = this.redisPrivClient.connect();
		List<String> listaPartesJson = redisConnection.sync().hvals("listaPartes");
		redisConnection.close();
		return listaPartesJson;
	}
	@Override
	public void setParte(String uuidParte, String recordParteJson) {
		StatefulRedisConnection redisConnection = this.redisPrivClient.connect();
		redisConnection.sync().hset("listaPartes", uuidParte, recordParteJson);
		redisConnection.close();
	}
	@Override
	public void delParte(String uuidParte) {
		StatefulRedisConnection redisConnection = this.redisPrivClient.connect();
		redisConnection.sync().hdel("listaPartes", uuidParte);
		redisConnection.close();
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

