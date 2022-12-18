package blender.distributed.Gateway;

import blender.distributed.Enums.EServicio;
import blender.distributed.Gateway.Servidor.GatewayClienteAction;
import blender.distributed.Gateway.Servidor.GatewayServidorAction;
import blender.distributed.Gateway.Servidor.GatewayWorkerAction;
import blender.distributed.Gateway.Threads.RefreshListaServidoresThread;
import blender.distributed.Gateway.Threads.RefreshListaWorkersThread;
import blender.distributed.Records.RGateway;
import blender.distributed.Records.RServidor;
import blender.distributed.Gateway.Threads.SendPingAliveThread;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import io.github.cdimascio.dotenv.Dotenv;

import static blender.distributed.SharedTools.Tools.getPublicIp;

public class Gateway {
	//General settings
	private static final Logger log = LoggerFactory.getLogger(Gateway.class);
	private String myPublicIp = getPublicIp(this.log);
	//RMI
	private int rmiPort;
	private static Registry registry;

	//redis related
	private String redisPrivURI;
	private String redisPubWriteURI;
	RedisClient redisPrivClient;
	RedisClient redisPubClient;
	List<RServidor> listaServidores = new ArrayList<>();
	boolean flushDb = false;
	Dotenv dotenv = Dotenv.load();
	Gson gson = new Gson();
	Type RListaServidorType = new TypeToken<List<RServidor>>(){}.getType();
	String uuid;
	public Gateway(boolean flushDb) {
		this.flushDb = flushDb;
		readConfigFile();
		runRedisPrivClient();
		runRMIGateway(this.rmiPort);
		runRedisPubClient();
		createThreadRefreshListaServidores();
		createThreadRefreshListaWorkers();
		createThreadSendPingAlive();
	}

	private void createThreadRefreshListaServidores() {
		RefreshListaServidoresThread listServT = new RefreshListaServidoresThread(this.listaServidores, this.redisPrivClient, this.log);
		Thread threadListaT = new Thread(listServT);
		threadListaT.start();
	}

	private void createThreadRefreshListaWorkers() {
		RefreshListaWorkersThread listaWorkersT = new RefreshListaWorkersThread(this.redisPrivClient, this.log);
		Thread threadAliveT = new Thread(listaWorkersT);
		threadAliveT.start();
	}

	private void createThreadSendPingAlive() {
		SendPingAliveThread aliveT = new SendPingAliveThread(this.redisPubClient, this.uuid, this.myPublicIp, this.rmiPort, this.log);
		Thread threadAliveT = new Thread(aliveT);
		threadAliveT.start();
	}

	private void runRMIGateway(int rmiPort) {
		try {
			System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true"); // renegotiation process is disabled by default.. Without this can't run two clients rmi on same machine like worker and client.
			System.setProperty("java.rmi.server.hostname", this.myPublicIp);
			log.info("Levantando servidor RMI en puerto " + rmiPort);
			registry = LocateRegistry.createRegistry(rmiPort);

			GatewayClienteAction gatewayClienteAction = new GatewayClienteAction(this.listaServidores, this.log);
			GatewayWorkerAction gatewayWorkerAction = new GatewayWorkerAction(this.listaServidores, this.log);
			GatewayServidorAction gatewayServidorAction = new GatewayServidorAction(this.redisPrivClient, this.listaServidores, this.log);

			UnicastRemoteObject.exportObject(gatewayClienteAction, rmiPort);
			UnicastRemoteObject.exportObject(gatewayWorkerAction, rmiPort);
			UnicastRemoteObject.exportObject(gatewayServidorAction, rmiPort);

			registry.rebind(EServicio.CLIENTE_ACTION.name(), gatewayClienteAction);
			registry.rebind(EServicio.WORKER_ACTION.name(), gatewayWorkerAction);
			registry.rebind(EServicio.SERVIDOR_ACTION.name(), gatewayServidorAction);

			log.info("Gateway RMI " + registry.toString());
			log.info("\t -> Para Clientes: " + EServicio.CLIENTE_ACTION.name());
			log.info("\t -> Para Workers: " + EServicio.WORKER_ACTION.name());
			log.info("\t -> Para Servidores: " + EServicio.SERVIDOR_ACTION.name());
			this.rmiPort = rmiPort;
		} catch (RemoteException e) {
			log.error("Error: Puerto " + rmiPort + " en uso.");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}
			runRMIGateway(++rmiPort);
		}
	}

	private void readConfigFile() {
		Map config;
		try {
			InputStream stream = this.getClass().getClassLoader().getResourceAsStream("Gateway/config.json");
			config = gson.fromJson(IOUtils.toString(stream, "UTF-8"), Map.class);

			Map rmi = (Map) config.get("rmi");
			this.rmiPort = Integer.valueOf(rmi.get("initialPortRmi").toString());

			this.redisPrivURI = "redis://"+dotenv.get("REDIS_PRIVATE_USER")+":"+dotenv.get("REDIS_PRIVATE_PASS")+"@"+dotenv.get("REDIS_PRIVATE_IP")+":"+dotenv.get("REDIS_PRIVATE_PORT");

			this.redisPubWriteURI = "redis://"+dotenv.get("REDIS_PUBLIC_WRITE_USER")+":"+dotenv.get("REDIS_PUBLIC_WRITE_PASS")+"@"+dotenv.get("REDIS_PUBLIC_IP")+":"+dotenv.get("REDIS_PUBLIC_PORT");

		} catch (IOException e) {
			log.error("Error: " + e.getMessage());
		}
	}

	private void runRedisPubClient() {
		this.redisPubClient = RedisClient.create(this.redisPubWriteURI);
		StatefulRedisConnection redisConnection = this.redisPubClient.connect();
		log.info("Conectado a Redis Público exitosamente.");
		RedisCommands commands = redisConnection.sync();
		if(flushDb) commands.flushdb();
		this.uuid = UUID.randomUUID().toString();
		commands.hset("listaGateways", this.uuid, gson.toJson(new RGateway(this.uuid, this.myPublicIp, this.rmiPort, ZonedDateTime.now().toInstant().toEpochMilli())));
		log.info("Gateway Iniciado -> " + this.uuid);
		redisConnection.close();
	}

	private void runRedisPrivClient() {
		this.redisPrivClient = RedisClient.create(this.redisPrivURI);
		StatefulRedisConnection redisConnection = this.redisPrivClient.connect();
		log.info("Conectado a Redis Privado exitosamente.");
		RedisCommands commands = redisConnection.sync();
		this.listaServidores = gson.fromJson(String.valueOf(commands.hvals("listaServidores")), RListaServidorType);
		if(this.flushDb) commands.flushdb();
		redisConnection.close();
	}

	public static void main(String[] args) {
		new Gateway(false);
	}
}
