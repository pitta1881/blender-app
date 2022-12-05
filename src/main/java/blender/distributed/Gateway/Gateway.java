package blender.distributed.Gateway;

import blender.distributed.Gateway.Servidor.GatewayClienteAction;
import blender.distributed.Gateway.Servidor.GatewayServidorAction;
import blender.distributed.Gateway.Servidor.GatewayWorkerAction;
import blender.distributed.Gateway.Servidor.IGatewayServidorAction;
import blender.distributed.Gateway.Threads.RefreshListaServidoresThread;
import blender.distributed.Gateway.Threads.RefreshListaWorkersThread;
import blender.distributed.Records.RGateway;
import blender.distributed.Records.RServidor;
import blender.distributed.Servidor.Cliente.IClienteAction;
import blender.distributed.Servidor.Worker.IWorkerAction;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.*;
import java.lang.reflect.Type;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Gateway {
	//General settings
	Logger log = LoggerFactory.getLogger(Gateway.class);
	private String myIp;

	//RMI
	private int rmiPortForClientes;
	private int rmiPortForWorkers;
	private int rmiPortForServidores;
	Registry registryCli;
	Registry registryWk;
	Registry registrySv;
	private IClienteAction remoteCliente;
	private IWorkerAction remoteWorker;
	private IGatewayServidorAction remoteServidor;

	//redis related
	private String redisPrivURI;
	private String redisPubWriteURI;
	RedisClient redisPrivClient;
	List<RServidor> listaServidores = new ArrayList<>();
	boolean flushDb = false;
	Gson gson = new Gson();
	Type RListaServidorType = new TypeToken<List<RServidor>>(){}.getType();
	public Gateway(boolean flushDb) {
		this.flushDb = flushDb;
		MDC.put("log.name", this.getClass().getSimpleName());
		readConfigFile();
		runRedisPrivClient();
		runRMIGateway(this.rmiPortForClientes, this.rmiPortForWorkers, this.rmiPortForServidores);
		runRedisPubClient();
		createThreadRefreshListaServidores();
		createThreadRefreshListaWorkers();
	}

	private void createThreadRefreshListaServidores() {
		RefreshListaServidoresThread listServT = new RefreshListaServidoresThread(this.listaServidores, this.redisPrivClient);
		Thread threadListaT = new Thread(listServT);
		threadListaT.start();
	}

	private void createThreadRefreshListaWorkers() {
		RefreshListaWorkersThread listaWorkersT = new RefreshListaWorkersThread(this.redisPrivClient);
		Thread threadAliveT = new Thread(listaWorkersT);
		threadAliveT.start();
	}

	private void runRMIGateway(int rmiPortForClientes, int rmiPortForWorkers, int rmiPortForServidores) {
		try {
			System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true"); // renegotiation process is disabled by default.. Without this can't run two clients rmi on same machine like worker and client.
			log.info("Levantando gateway RMI...");
			registryCli = LocateRegistry.createRegistry(rmiPortForClientes);
			registryWk = LocateRegistry.createRegistry(rmiPortForWorkers);
			registrySv = LocateRegistry.createRegistry(rmiPortForServidores);

			remoteCliente = (IClienteAction) UnicastRemoteObject.exportObject(new GatewayClienteAction(this.listaServidores),0);
			remoteWorker = (IWorkerAction) UnicastRemoteObject.exportObject(new GatewayWorkerAction(this.listaServidores),0);
			remoteServidor = (IGatewayServidorAction) UnicastRemoteObject.exportObject(new GatewayServidorAction(this.redisPrivClient, this.listaServidores),0);

			registryCli.rebind("clienteAction", remoteCliente);
			registryWk.rebind("workerAction", remoteWorker);
			registrySv.rebind("servidorAction", remoteServidor);

			log.info("Gateway RMI");
			log.info("\t -> Para Clientes: " + registryCli.toString());
			log.info("\t -> Para Workers: " + registryWk.toString());
			log.info("\t -> Para Servidores: " + registrySv.toString());
			this.rmiPortForClientes = rmiPortForClientes;
			this.rmiPortForWorkers = rmiPortForWorkers;
			this.rmiPortForServidores = rmiPortForServidores;
		} catch (RemoteException e) {
			log.error("Error: Puertos " + rmiPortForClientes + "(Clientes), " + rmiPortForWorkers + "(Workers) y " + rmiPortForServidores + "(Servidores) en uso.");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}
			runRMIGateway(++rmiPortForClientes, ++rmiPortForWorkers, ++rmiPortForServidores);
		}
	}

	private void readConfigFile() {
		Map config;
		try {
			InputStream stream = Gateway.class.getClassLoader().getResourceAsStream("Gateway/config.json");
			config = gson.fromJson(IOUtils.toString(stream, "UTF-8"), Map.class);

			Map server = (Map) config.get("gateway");
			this.myIp = server.get("ip").toString();

			Map rmi = (Map) config.get("rmi");
			this.rmiPortForClientes = Integer.valueOf(rmi.get("initialPortForClientes").toString());
			this.rmiPortForWorkers = Integer.valueOf(rmi.get("initialPortForWorkers").toString());
			this.rmiPortForServidores = Integer.valueOf(rmi.get("initialPortForServidores").toString());

			Map redisPriv = (Map) config.get("redis_priv");
			this.redisPrivURI = redisPriv.get("uri").toString();

			Map redisPub = (Map) config.get("redis_pub");
			this.redisPubWriteURI = redisPub.get("uri").toString();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void runRedisPubClient() {
		RedisClient redisPubClient = RedisClient.create(this.redisPubWriteURI);
		StatefulRedisConnection redisConnection = redisPubClient.connect();
		log.info("Conectado a Redis Público exitosamente.");
		RedisCommands commands = redisConnection.sync();
		if(flushDb) commands.flushdb();
		String uuid = UUID.randomUUID().toString();
		commands.hset("listaGateways", uuid, gson.toJson(new RGateway(uuid, this.myIp, this.rmiPortForClientes, this.rmiPortForWorkers, this.rmiPortForServidores)));
		log.info("Iniciando Gateway -> " + uuid);
		redisConnection.close();
		redisPubClient.shutdown();
	}

	private void runRedisPrivClient() {
		this.redisPrivClient = RedisClient.create(this.redisPrivURI);
		log.info("Conectado a Redis Privado exitosamente.");
		StatefulRedisConnection redisConnection = this.redisPrivClient.connect();
		RedisCommands commands = redisConnection.sync();
		this.listaServidores = gson.fromJson(String.valueOf(commands.hvals("listaServidores")), RListaServidorType);
		if(this.flushDb) commands.flushdb();
		redisConnection.close();
	}

	public static void main(String[] args) {
		new Gateway(true);
	}
}
