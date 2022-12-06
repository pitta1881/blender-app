package blender.distributed.Servidor;

import blender.distributed.Gateway.Gateway;
import blender.distributed.Records.RGateway;
import blender.distributed.Servidor.Cliente.ClienteAction;
import blender.distributed.Servidor.Cliente.IClienteAction;
import blender.distributed.Servidor.Threads.SendPingAliveThread;
import blender.distributed.Servidor.Worker.IWorkerAction;
import blender.distributed.Servidor.Worker.WorkerAction;
import blender.distributed.SharedTools.DirectoryTools;
import blender.distributed.SharedTools.RefreshListaGatewaysThread;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.cdimascio.dotenv.Dotenv;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;

import static blender.distributed.Gateway.Tools.connectRandomGatewayRMIForServidor;

public class Servidor {
	//General settings
	Logger log = LoggerFactory.getLogger(Servidor.class);
	String appDir = System.getProperty("user.dir") + "/app/";
	String serverDirectory = appDir + "Servidor/";
	String singleServerDir;

	//RMI
	private int rmiPortForClientes;
	private int rmiPortForWorkers;
	Registry registryCli;
	Registry registrySv;
	private IClienteAction remoteCliente;
	private IWorkerAction remoteWorker;

	private String redisPubURI;
	List<RGateway> listaGateways;
	String uuid;
	RedisClient redisPubClient;
	Dotenv dotenv = Dotenv.load();


	public Servidor() {
		MDC.put("log.name", this.getClass().getSimpleName());
		readConfigFile();
		runRedisPubClient();
		runRMIServer(this.rmiPortForClientes, this.rmiPortForWorkers);
		createThreadRefreshListaGateways();
		helloGateway();
		createThreadSendPingAlive();
	}

	private void createThreadSendPingAlive() {
		SendPingAliveThread aliveT = new SendPingAliveThread(this.uuid, this.listaGateways, this.rmiPortForClientes, this.rmiPortForWorkers);
		Thread threadAliveT = new Thread(aliveT);
		threadAliveT.start();
	}
	private void createThreadRefreshListaGateways() {
		RefreshListaGatewaysThread listaGatewaysT = new RefreshListaGatewaysThread(this.listaGateways, this.redisPubClient);
		Thread threadAliveT = new Thread(listaGatewaysT);
		threadAliveT.start();
	}

	private void runRMIServer(int rmiPortForClientes, int rmiPortForWorkers) {
		try {
			System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true"); // renegotiation process is disabled by default.. Without this can't run two clients rmi on same machine like worker and client.
			createSingleServerDir(rmiPortForWorkers);
			log.info("Levantando servidor RMI en puertos " + rmiPortForClientes + "(Clientes) y " + rmiPortForWorkers + "(Workers)");
			registryCli = LocateRegistry.createRegistry(rmiPortForClientes);
			registrySv = LocateRegistry.createRegistry(rmiPortForWorkers);

			remoteCliente = (IClienteAction) UnicastRemoteObject.exportObject(new ClienteAction(this.listaGateways),0);
			remoteWorker = (IWorkerAction) UnicastRemoteObject.exportObject(new WorkerAction(this.listaGateways, this.singleServerDir),0);

			registryCli.rebind("clienteAction", remoteCliente);
			registrySv.rebind("workerAction", remoteWorker);

			log.info("Servidor RMI:");
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
			InputStream stream = Gateway.class.getClassLoader().getResourceAsStream("Servidor/config.json");
			config = gson.fromJson(IOUtils.toString(stream, "UTF-8"), Map.class);

			Map rmi = (Map) config.get("rmi");
			this.rmiPortForClientes = Integer.valueOf(rmi.get("initialPortForClientes").toString());
			this.rmiPortForWorkers = Integer.valueOf(rmi.get("initialPortForWorkers").toString());

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
		DirectoryTools.checkOrCreateFolder(appDir.getAbsolutePath());
		DirectoryTools.checkOrCreateFolder(serverDir.getAbsolutePath());
		this.singleServerDir = singleServerFileDir.getAbsolutePath();
		DirectoryTools.checkOrCreateFolder(this.singleServerDir);
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
			String uuid = connectRandomGatewayRMIForServidor(this.listaGateways).helloGatewayFromServidor(this.rmiPortForClientes, this.rmiPortForWorkers);
			if(!uuid.isEmpty()){
				log.info("Conectado a un Gateway. UUID Asignado: " + uuid);
				this.uuid = uuid;
			}
		} catch (RemoteException | NullPointerException e) {
			e.printStackTrace();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}
			helloGateway();
		}
	}

	public static void main(String[] args) {
		new Servidor();
	}
}
