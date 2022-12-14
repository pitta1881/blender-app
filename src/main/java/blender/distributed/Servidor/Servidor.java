package blender.distributed.Servidor;

import blender.distributed.Enums.EServicio;
import blender.distributed.Gateway.Servidor.IServidorAction;
import blender.distributed.Records.RGateway;
import blender.distributed.Servidor.Cliente.ClienteAction;
import blender.distributed.Servidor.Cliente.IClienteAction;
import blender.distributed.Servidor.Threads.SendPingAliveThread;
import blender.distributed.Servidor.Worker.IWorkerAction;
import blender.distributed.Servidor.Worker.WorkerAction;
import blender.distributed.SharedTools.DirectoryTools;
import blender.distributed.SharedTools.RefreshListaGatewaysThread;
import blender.distributed.SharedTools.Tools;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.cdimascio.dotenv.Dotenv;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;

import static blender.distributed.SharedTools.Tools.getPublicIp;

public class Servidor {
	//General settings
	private static final Logger log = LoggerFactory.getLogger(Servidor.class);
	String appDir = System.getProperty("user.dir") + "/app/";
	String serverDirectory = appDir + "Servidor/";
	String singleServerDir;
	private String myPublicIp = getPublicIp(this.log);

	//RMI
	private int rmiPort;
	private static Registry registry;
	private static IClienteAction remoteCliente;
	private static IWorkerAction remoteWorker;

	private String redisPubURI;
	List<RGateway> listaGateways;
	String uuid;
	RedisClient redisPubClient;
	Dotenv dotenv = Dotenv.load();
	int frameDivision;


	public Servidor() {
		readConfigFile();
		runRedisPubClient();
		runRMIServer(this.rmiPort);
		createThreadRefreshListaGateways();
		helloGateway();
		createThreadSendPingAlive();
	}

	private void createThreadSendPingAlive() {
		SendPingAliveThread aliveT = new SendPingAliveThread(this.uuid, this.myPublicIp, this.listaGateways, this.rmiPort, this.log);
		Thread threadAliveT = new Thread(aliveT);
		threadAliveT.start();
	}
	private void createThreadRefreshListaGateways() {
		RefreshListaGatewaysThread listaGatewaysT = new RefreshListaGatewaysThread(this.listaGateways, this.redisPubClient, this.log);
		Thread threadAliveT = new Thread(listaGatewaysT);
		threadAliveT.start();
	}

	private void runRMIServer(int rmiPort) {
		try {
			System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true"); // renegotiation process is disabled by default.. Without this can't run two clients rmi on same machine like worker and client.
			System.setProperty("java.rmi.server.hostname", this.myPublicIp);
			createSingleServerDir(rmiPort);
			log.info("Levantando servidor RMI en puerto " + rmiPort);

			registry = LocateRegistry.createRegistry(rmiPort);

			ClienteAction clienteAction = new ClienteAction(this.listaGateways, this.frameDivision, this.log);
			WorkerAction workerAction = new WorkerAction(this.listaGateways, this.singleServerDir, this.log);

			remoteCliente = (IClienteAction) UnicastRemoteObject.exportObject(clienteAction, rmiPort);
			remoteWorker = (IWorkerAction) UnicastRemoteObject.exportObject(workerAction, rmiPort);

			registry.rebind(EServicio.CLIENTE_ACTION.name(), remoteCliente);
			registry.rebind(EServicio.WORKER_ACTION.name(), remoteWorker);

			log.info("Servidor RMI " + registry.toString());
			log.info("\t -> Para Clientes: " + EServicio.CLIENTE_ACTION.name());
			log.info("\t -> Para Workers: " + EServicio.WORKER_ACTION.name());
			this.rmiPort = rmiPort;
		} catch (RemoteException e) {
			log.error("Error: Puerto " + rmiPort + " en uso.");
			runRMIServer(++rmiPort);
		}
	}

	private void readConfigFile() {
		Gson gson = new Gson();
		Map config;
		try {
			InputStream stream = this.getClass().getClassLoader().getResourceAsStream("Servidor/config.json");
			config = gson.fromJson(IOUtils.toString(stream, "UTF-8"), Map.class);

			Map rmi = (Map) config.get("rmi");
			this.rmiPort = Integer.valueOf(rmi.get("initialPortRmi").toString());

			Map tools = (Map) config.get("tools");
			this.frameDivision = Integer.valueOf(tools.get("frameDivision").toString());

			this.redisPubURI = "redis://"+dotenv.get("REDIS_PUBLIC_USER")+":"+dotenv.get("REDIS_PUBLIC_PASS")+"@"+dotenv.get("REDIS_PUBLIC_IP")+":"+dotenv.get("REDIS_PUBLIC_PORT");

		} catch (IOException e) {
			log.error("Error Archivo Config!");
		}
	}

	private void createSingleServerDir(int rmiPort){
		int serverNumber = rmiPort - 9150;
		File appDir = new File(this.appDir);
		File serverDir = new File(this.serverDirectory);
		File singleServerFileDir = new File(this.serverDirectory + "/server9x5" + serverNumber);
		DirectoryTools.checkOrCreateFolder(appDir.getAbsolutePath(), this.log);
		DirectoryTools.checkOrCreateFolder(serverDir.getAbsolutePath(), this.log);
		this.singleServerDir = singleServerFileDir.getAbsolutePath();
		DirectoryTools.checkOrCreateFolder(this.singleServerDir, this.log);
	}

	private void runRedisPubClient() {
		this.redisPubClient = RedisClient.create(this.redisPubURI);
		StatefulRedisConnection redisConnection = this.redisPubClient.connect();
		log.info("Conectado a Redis P??blico exitosamente.");
		RedisCommands commands = redisConnection.sync();
		this.listaGateways = new Gson().fromJson(String.valueOf(commands.hvals("listaGateways")), new TypeToken<List<RGateway>>(){}.getType());
		redisConnection.close();
	}

	private void helloGateway() {
		try {
			Thread.sleep(1000);
			String uuid = Tools.<IServidorAction>connectRandomGatewayRMI(this.listaGateways, EServicio.SERVIDOR_ACTION, -1, this.log).helloGatewayFromServidor(this.myPublicIp, this.rmiPort);
			if(!uuid.isEmpty()){
				log.info("Conectado a un Gateway. UUID Asignado: " + uuid);
				this.uuid = uuid;
			}
		} catch (InterruptedException | RemoteException | NullPointerException e) {
			log.error("Error: " + e.getMessage());
			helloGateway();
		}
	}

	public static void main(String[] args) {
		new Servidor();
	}
}
