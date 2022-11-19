package blender.distributed.Gateway.Servidor;

import blender.distributed.Gateway.PairIpPortCPortW;
import blender.distributed.Gateway.PairParteLastping;
import blender.distributed.Servidor.SerializedObjectCodec;
import blender.distributed.Servidor.Trabajo.Trabajo;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.rmi.server.ServerNotActiveException;
import java.util.ArrayList;
import java.util.List;

import static java.rmi.server.RemoteServer.getClientHost;

public class GatewayServidorAction implements IGatewayServidorAction {
	Logger log = LoggerFactory.getLogger(GatewayServidorAction.class);
	RedisClient redisClient;
	ArrayList<PairIpPortCPortW> listaServidores;

	public GatewayServidorAction(RedisClient redisClient, ArrayList<PairIpPortCPortW> listaServidores) {
		MDC.put("log.name", GatewayServidorAction.class.getSimpleName());
		this.redisClient = redisClient;
		this.listaServidores = listaServidores;
	}

	@Override
	public String helloGateway(int rmiPortForClientes, int rmiPortForWorkers) {
		synchronized (this.listaServidores){
			try {
				PairIpPortCPortW pipp = new PairIpPortCPortW(getClientHost(),rmiPortForClientes, rmiPortForWorkers);
				log.info("Registrando nuevo servidor: " + pipp);
				this.listaServidores.add(pipp);
			} catch (ServerNotActiveException e) {
				throw new RuntimeException(e);
			}
		}
		return "OK";
	}

	@Override
	public PairParteLastping getWorker(String workerName) {
		StatefulRedisConnection<String, Object> redisConnection = this.redisClient.connect(new SerializedObjectCodec());
		PairParteLastping workerRecord = (PairParteLastping) redisConnection.sync().hget("listaWorkers", workerName);
		redisConnection.close();
		return workerRecord;
	}

	@Override
	public void setWorker(String workerName, PairParteLastping workerRecord) {
		StatefulRedisConnection<String, Object> redisConnection = this.redisClient.connect(new SerializedObjectCodec());
		redisConnection.sync().hset("listaWorkers", workerName, workerRecord);
		redisConnection.close();
	}

	@Override
	public void delWorker(String workerName) {
		StatefulRedisConnection<String, Object> redisConnection = this.redisClient.connect(new SerializedObjectCodec());
		redisConnection.sync().hdel("listaWorkers", workerName);
		redisConnection.close();
	}

	@Override
	public Trabajo getTrabajo(String workId) {
		StatefulRedisConnection<String, Object> redisConnection = this.redisClient.connect(new SerializedObjectCodec());
		Trabajo trabajo = (Trabajo) redisConnection.sync().hget("listaTrabajos", workId);
		redisConnection.close();
		return trabajo;
	}
	@Override
	public List<Object> getAllTrabajos() {
		StatefulRedisConnection<String, Object> redisConnection = this.redisClient.connect(new SerializedObjectCodec());
		List<Object> trabajos = redisConnection.sync().hvals("listaTrabajos");
		redisConnection.close();
		return trabajos;
	}
	@Override
	public void setTrabajo(Trabajo work) {
		StatefulRedisConnection<String, Object> redisConnection = this.redisClient.connect(new SerializedObjectCodec());
		redisConnection.sync().hset("listaTrabajos", work.getId(), work);
		redisConnection.close();
	}

	@Override
	public void delTrabajo(String workId) {
		StatefulRedisConnection<String, Object> redisConnection = this.redisClient.connect(new SerializedObjectCodec());
		redisConnection.sync().hdel("listaTrabajos", workId);
		redisConnection.close();
	}

}
