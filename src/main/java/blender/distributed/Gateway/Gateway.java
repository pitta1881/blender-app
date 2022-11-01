package blender.distributed.Gateway;

import blender.distributed.Gateway.Servidor.IServidorClientAction;
import blender.distributed.Gateway.Servidor.IServidorWorkerAction;
import blender.distributed.Gateway.Servidor.ServidorClienteAction;
import blender.distributed.Gateway.Servidor.ServidorWorkerAction;
import com.google.gson.Gson;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Map;

public class Gateway {
	//General settings
	Logger log = LoggerFactory.getLogger(Gateway.class);
	String gatewayDirectory = System.getProperty("user.dir")+"\\src\\main\\resources\\Gateway\\";
	private String myIp;

	//RMI
	private int rmiPortForClientes;
	private int rmiPortForWorkers;
	Registry registryCli;
	Registry registrySv;
	private IServidorClientAction remoteCliente;
	private IServidorWorkerAction remoteWorker;

	JedisPool pool;
	//redis related
	private String redisIp;
	private int redisPort;
	private String redisPassword;
	byte[] listaWorkersByte = SerializationUtils.serialize("listaWorkers");

	public Gateway() {
		MDC.put("log.name", this.getClass().getSimpleName());
		readConfigFile();
		try {
			runRMIServer();
			runRedisClient();
			while(true){
				Thread.sleep(3000);
				try (Jedis jedis = this.pool.getResource()) {
					Map<byte[], byte[]> listaWorkers = jedis.hgetAll(this.listaWorkersByte);
					listaWorkers.forEach((workerNameByte, parParteLastpingByte) -> {
						PairParteLastping plp = SerializationUtils.deserialize(parParteLastpingByte);
						//Checkeo si se cayo un nodo
						String workerNameString = SerializationUtils.deserialize(workerNameByte);
						LocalTime timeLastPing = plp.lastPing();
						int differenceLastKnownPing = (int) Duration.between(timeLastPing, LocalTime.now()).getSeconds();
						if (differenceLastKnownPing > 7) {
							//plp.ptp().parte().setStatus(TrabajoStatus.TO_DO); conseguir el trabajo de la otra lista
							jedis.hdel(this.listaWorkersByte, workerNameByte);
							log.error("Eliminando al nodo " + workerNameString + ". Motivo time-out de " + differenceLastKnownPing + " segundos.");
						}
					jedis.close();
					});
				}
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private void runRMIServer() throws RemoteException {
		System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true"); // renegotiation process is disabled by default.. Without this can't run two clients rmi on same machine like worker and client.
		log.info("Levantando gateway RMI...");
		registryCli = LocateRegistry.createRegistry(this.rmiPortForClientes);
		registrySv = LocateRegistry.createRegistry(this.rmiPortForWorkers);

		remoteCliente = (IServidorClientAction) UnicastRemoteObject.exportObject(new ServidorClienteAction(this.myIp),0);
		remoteWorker = (IServidorWorkerAction) UnicastRemoteObject.exportObject(new ServidorWorkerAction(this.myIp),0);
		remoteCliente.setPrimaryServerPort(9101);
		remoteWorker.setPrimaryServerPort(9201);

		registryCli.rebind("clientAction", remoteCliente);
		registrySv.rebind("workerAction", remoteWorker);

		log.info("Gateway RMI");
		log.info("\t -> Para Clientes: " + registryCli.toString());
		log.info("\t -> Para Workers: " + registrySv.toString());
	}

	private void readConfigFile() {
		Gson gson = new Gson();
		Map config;
		try {
			config = gson.fromJson(new FileReader(this.gatewayDirectory +"config.json"), Map.class);
			
			Map server = (Map) config.get("gateway");
			this.myIp = server.get("ip").toString();

			Map rmi = (Map) config.get("rmi");
			this.rmiPortForClientes = Integer.valueOf(rmi.get("portForClientes").toString());
			this.rmiPortForWorkers = Integer.valueOf(rmi.get("portForWorkers").toString());

			Map redis = (Map) config.get("redis");
			this.redisIp = redis.get("ip").toString();
			this.redisPort = Integer.valueOf(redis.get("port").toString());
			this.redisPassword = redis.get("password").toString();

		} catch (IOException e) {
		}
	}

	private void runRedisClient() {
		this.pool = new JedisPool(new JedisPoolConfig(), this.redisIp, this.redisPort, Protocol.DEFAULT_TIMEOUT, this.redisPassword);
		try (Jedis jedis = this.pool.getResource()) {
			jedis.flushAll();
		}
	}

	public static void main(String[] args) {
		new Gateway();
	}
}
