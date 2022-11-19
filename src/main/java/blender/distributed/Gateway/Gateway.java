package blender.distributed.Gateway;

import blender.distributed.Gateway.Servidor.*;
import com.google.gson.Gson;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Map;

public class Gateway {
	//General settings
	Logger log = LoggerFactory.getLogger(Gateway.class);
	String gatewayDirectory = System.getProperty("user.dir")+"\\src\\main\\resources\\Gateway\\";
	private String myIp;

	//RMI
	private int rmiPortForClientes;
	private int rmiPortForWorkers;
	private int rmiPortForServidores;
	Registry registryCli;
	Registry registryWk;
	Registry registrySv;
	private IGatewayClientAction remoteCliente;
	private IGatewayWorkerAction remoteWorker;
	private IGatewayServidorAction remoteServidor;

	//redis related
	private String redisIp;
	private int redisPort;
	private String redisPassword;
	RedisClient redisClient;
	ArrayList<PairIpPortCPortW> listaServidores = new ArrayList<>();

	public Gateway() {
		MDC.put("log.name", this.getClass().getSimpleName());
		readConfigFile();
		try {
			runRedisClient();
			runRMIServer();
			while(true){
				Thread.sleep(3000);
				/*
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
				 */
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
		registryWk = LocateRegistry.createRegistry(this.rmiPortForWorkers);
		registrySv = LocateRegistry.createRegistry(this.rmiPortForServidores);

		remoteCliente = (IGatewayClientAction) UnicastRemoteObject.exportObject(new GatewayClienteAction(this.listaServidores),0);
		remoteWorker = (IGatewayWorkerAction) UnicastRemoteObject.exportObject(new GatewayWorkerAction(this.listaServidores),0);
		remoteServidor = (IGatewayServidorAction) UnicastRemoteObject.exportObject(new GatewayServidorAction(this.redisClient, this.listaServidores),0);

		registryCli.rebind("clientAction", remoteCliente);
		registryWk.rebind("workerAction", remoteWorker);
		registrySv.rebind("servidorAction", remoteServidor);

		log.info("Gateway RMI");
		log.info("\t -> Para Clientes: " + registryCli.toString());
		log.info("\t -> Para Workers: " + registryWk.toString());
		log.info("\t -> Para Servidores: " + registrySv.toString());
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
			this.rmiPortForServidores = Integer.valueOf(rmi.get("portForServidores").toString());

			Map redis = (Map) config.get("redis");
			this.redisIp = redis.get("ip").toString();
			this.redisPort = Integer.valueOf(redis.get("port").toString());
			this.redisPassword = redis.get("password").toString();

		} catch (IOException e) {
		}
	}

	private void runRedisClient() {
		this.redisClient = RedisClient.create("redis://"+this.redisPassword+"@"+this.redisIp+":"+this.redisPort);
		log.info("Conectado a Redis exitosamente.");
		StatefulRedisConnection redisConnection = redisClient.connect();
		redisConnection.sync().flushdb();
		redisConnection.close();
	}

	public static void main(String[] args) {
		new Gateway();
	}
}
