package blender.distributed.Servidor;

import blender.distributed.Servidor.Cliente.ClienteAction;
import blender.distributed.Servidor.Cliente.IClientAction;
import blender.distributed.Servidor.FTP.FTPAction;
import blender.distributed.Servidor.FTP.IFTPAction;
import blender.distributed.Servidor.FTP.ServerFtp;
import blender.distributed.Servidor.Trabajo.Trabajo;
import blender.distributed.Servidor.Worker.IWorkerAction;
import blender.distributed.Servidor.Worker.WorkerAction;
import blender.distributed.SharedTools.DirectoryTools;
import com.google.gson.Gson;
import io.lettuce.core.RedisClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Map;

public class Servidor {
	//General settings
	Logger log = LoggerFactory.getLogger(Servidor.class);
	String serverDirectory = System.getProperty("user.dir")+"\\src\\main\\resources\\Servidor\\";
	String singleServerDir;
	String myFTPDirectory;
	private String myIp;
	private ArrayList<Trabajo> listaTrabajos = new ArrayList<>();

	//RMI
	private int rmiPortForClientes;
	private int rmiPortForWorkers;
	Registry registryCli;
	Registry registrySv;
	private IClientAction remoteCliente;
	private IFTPAction remoteFtpMan;
	private IWorkerAction remoteWorker;

	//Ftp Related
	private int ftpPort;
	ServerFtp ftp;

	//redis related
	private String redisIp;
	private int redisPort;
	private String redisPassword;
	RedisClient redisClient;


	public Servidor() {
		MDC.put("log.name", this.getClass().getSimpleName());
		readConfigFile();
		runFTPServer();
		runRedisClient();
		runRMIServer(this.rmiPortForClientes, this.rmiPortForWorkers);
	}

	private void runRMIServer(int rmiPortForClientes, int rmiPortForWorkers) {
		try {
			System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true"); // renegotiation process is disabled by default.. Without this can't run two clients rmi on same machine like worker and client.
			createSingleServerDir(rmiPortForWorkers);
			log.info("Levantando servidor RMI en puertos " + rmiPortForClientes + "(Clientes) y " + rmiPortForWorkers + "(Workers)");
			registryCli = LocateRegistry.createRegistry(rmiPortForClientes);
			registrySv = LocateRegistry.createRegistry(rmiPortForWorkers);

			remoteFtpMan = (IFTPAction) UnicastRemoteObject.exportObject(new FTPAction(this.ftpPort, this.ftp),0);
			remoteCliente = (IClientAction) UnicastRemoteObject.exportObject(new ClienteAction(this.redisClient),0);
			remoteWorker = (IWorkerAction) UnicastRemoteObject.exportObject(new WorkerAction(this.redisClient, this.singleServerDir),0);

			registrySv.rebind("ftpAction", remoteFtpMan);
			registryCli.rebind("clientAction", remoteCliente);
			registrySv.rebind("workerAction", remoteWorker);

			log.info("Servidor RMI:");
			log.info("\t -> Para Clientes: " + registryCli.toString());
			log.info("\t -> Para Workers: " + registrySv.toString());
		} catch (RemoteException e) {
			log.error("Error: Puertos " + rmiPortForClientes + "(Clientes) y " + rmiPortForWorkers + "(Workers) en uso.");
			runRMIServer(++rmiPortForClientes, ++rmiPortForWorkers);
		}
	}

	private void readConfigFile() {
		Gson gson = new Gson();
		Map config;
		try {
			config = gson.fromJson(new FileReader(this.serverDirectory+"config.json"), Map.class);
			
			Map server = (Map) config.get("server");
			this.myIp = server.get("ip").toString();

			Map rmi = (Map) config.get("rmi");
			this.rmiPortForClientes = Integer.valueOf(rmi.get("initialPortForClientes").toString());
			this.rmiPortForWorkers = Integer.valueOf(rmi.get("initialPortForWorkers").toString());

			Map ftp = (Map) config.get("ftp");
			this.ftpPort = Integer.valueOf(ftp.get("port").toString());
			this.myFTPDirectory = this.serverDirectory + ftp.get("directory").toString();

			Map redis = (Map) config.get("redis");
			this.redisIp = redis.get("ip").toString();
			this.redisPort = Integer.valueOf(redis.get("port").toString());
			this.redisPassword = redis.get("password").toString();
		} catch (IOException e) {
		}
	}

	private void runFTPServer() {
		this.ftp = new ServerFtp(this.ftpPort, this.myFTPDirectory);
		log.info("FTP Configurado correctamente. Listo para usar en puerto:"+this.ftpPort+". Compartiendo carpeta: "+this.myFTPDirectory);
	}

	private void createSingleServerDir(int portWorkerUsed){
		int serverNumber = portWorkerUsed - 9200;
		File singleServerFileDir = new File(this.serverDirectory + "\\server9x0" + serverNumber);
		this.singleServerDir = singleServerFileDir.getAbsolutePath();
		DirectoryTools.checkOrCreateFolder(this.singleServerDir);
	}

	private void runRedisClient() {
		this.redisClient = RedisClient.create("redis://"+this.redisPassword+"@"+this.redisIp+":"+this.redisPort);
		log.info("Conectado a Redis exitosamente.");
	}

	public static void main(String[] args) {
		new Servidor();
	}
}
