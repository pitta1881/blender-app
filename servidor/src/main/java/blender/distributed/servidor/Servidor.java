package blender.distributed.servidor;

import blender.distributed.servidor.Cliente.ClienteAction;
import blender.distributed.servidor.Threads.SendPingAliveThread;
import blender.distributed.servidor.Worker.WorkerAction;
import blender.distributed.shared.DirectoryTools;
import blender.distributed.shared.Interfaces.IClienteAction;
import blender.distributed.shared.Interfaces.IWorkerAction;
import blender.distributed.shared.Records.RGateway;
import blender.distributed.shared.RefreshListaGatewaysThread;
import ch.qos.logback.core.FileAppender;
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

import static blender.distributed.servidor.Tools.connectRandomGatewayRMIForServidor;
import static blender.distributed.shared.Tools.getPublicIp;


public class Servidor {
	//General settings
	private static final Logger log = LoggerFactory.getLogger(Servidor.class);
	String appDir = System.getProperty("user.dir") + "/app/";
	String serverDirectory = appDir + "Servidor/";
	String singleServerDir;
	private String myPublicIp = getPublicIp(this.log);

	//RMI
	private int rmiPortForClientes;
	private int rmiPortForWorkers;
	static Registry registryCli;
	static Registry registrySv;
	private static IClienteAction remoteCliente;
	private static IWorkerAction remoteWorker;

	private String redisPubURI;
	List<RGateway> listaGateways;
	String uuid;
	RedisClient redisPubClient;
	Dotenv dotenv = Dotenv.load();
	int frameDivision;

	/*
	 * This block prevents the Maven Shade plugin to remove the specified classes
	 */
	static {
		@SuppressWarnings ("unused") Class<?>[] classes = new Class<?>[] {
				FileAppender.class
		};
	}
	public Servidor() {
		readConfigFile();
		runRedisPubClient();
		runRMIServer(this.rmiPortForClientes, this.rmiPortForWorkers);
		createThreadRefreshListaGateways();
		helloGateway();
		createThreadSendPingAlive();
	}

	private void createThreadSendPingAlive() {
		SendPingAliveThread aliveT = new SendPingAliveThread(this.uuid, this.myPublicIp, this.listaGateways, this.rmiPortForClientes, this.rmiPortForWorkers, this.log);
		Thread threadAliveT = new Thread(aliveT);
		threadAliveT.start();
	}
	private void createThreadRefreshListaGateways() {
		RefreshListaGatewaysThread listaGatewaysT = new RefreshListaGatewaysThread(this.listaGateways, this.redisPubClient, this.log);
		Thread threadAliveT = new Thread(listaGatewaysT);
		threadAliveT.start();
	}

	private void runRMIServer(int rmiPortForClientes, int rmiPortForWorkers) {
		try {
			System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true"); // renegotiation process is disabled by default.. Without this can't run two clients rmi on same machine like worker and client.
			System.setProperty("java.rmi.server.hostname", this.myPublicIp);
			createSingleServerDir(rmiPortForWorkers);
			log.info("Levantando servidor RMI en puertos " + rmiPortForClientes + "(Clientes) y " + rmiPortForWorkers + "(Workers)");
			registryCli = LocateRegistry.createRegistry(rmiPortForClientes);
			registrySv = LocateRegistry.createRegistry(rmiPortForWorkers);

			remoteCliente = (IClienteAction) UnicastRemoteObject.exportObject(new ClienteAction(this.listaGateways, this.frameDivision, this.log),rmiPortForClientes);
			remoteWorker = (IWorkerAction) UnicastRemoteObject.exportObject(new WorkerAction(this.listaGateways, this.singleServerDir, this.log),rmiPortForWorkers);

			registryCli.rebind("clienteAction", remoteCliente);
			registrySv.rebind("workerAction", remoteWorker);

			log.info("blender.distributed.servidor.Servidor RMI:");
			log.info("\t -> Para Clientes: " + registryCli.toString());
			log.info("\t -> Para Workers: " + registrySv.toString());
			this.rmiPortForClientes = rmiPortForClientes;
			this.rmiPortForWorkers = rmiPortForWorkers;
		} catch (RemoteException e) {
			log.error("Error: Puertos " + rmiPortForClientes + "(Clientes) y " + rmiPortForWorkers + "(Workers) en uso.");
			runRMIServer(++rmiPortForClientes, ++rmiPortForWorkers);
		}
	}

	private void readConfigFile() {
		Gson gson = new Gson();
		Map config;
		try {
			InputStream stream = this.getClass().getClassLoader().getResourceAsStream("Servidor/config.json");
			config = gson.fromJson(IOUtils.toString(stream, "UTF-8"), Map.class);

			Map rmi = (Map) config.get("rmi");
			this.rmiPortForClientes = Integer.valueOf(rmi.get("initialPortForClientes").toString());
			this.rmiPortForWorkers = Integer.valueOf(rmi.get("initialPortForWorkers").toString());

			Map tools = (Map) config.get("tools");
			this.frameDivision = Integer.valueOf(tools.get("frameDivision").toString());

			this.redisPubURI = "redis://"+dotenv.get("REDIS_PUBLIC_USER")+":"+dotenv.get("REDIS_PUBLIC_PASS")+"@"+dotenv.get("REDIS_PUBLIC_IP")+":"+dotenv.get("REDIS_PUBLIC_PORT");

		} catch (IOException e) {
			log.error("Error Archivo Config!");
		}
	}

	private void createSingleServerDir(int portWorkerUsed){
		int serverNumber = portWorkerUsed - 9250;
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
		log.info("Conectado a Redis Público exitosamente.");
		RedisCommands commands = redisConnection.sync();
		this.listaGateways = new Gson().fromJson(String.valueOf(commands.hvals("listaGateways")), new TypeToken<List<RGateway>>(){}.getType());
		redisConnection.close();
	}

	private void helloGateway() {
		try {
			Thread.sleep(1000);
			String uuid = connectRandomGatewayRMIForServidor(this.listaGateways, this.log).helloGatewayFromServidor(this.myPublicIp, this.rmiPortForClientes, this.rmiPortForWorkers);
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
