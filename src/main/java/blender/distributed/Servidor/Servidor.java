package blender.distributed.Servidor;

import blender.distributed.Servidor.Cliente.ClienteAction;
import blender.distributed.Servidor.Cliente.IClientAction;
import blender.distributed.Servidor.FTP.FTPAction;
import blender.distributed.Servidor.FTP.IFTPAction;
import blender.distributed.Servidor.FTP.ServerFtp;
import blender.distributed.Servidor.Gateway.GatewayAction;
import blender.distributed.Servidor.Gateway.IGatewayAction;
import blender.distributed.Servidor.Trabajo.PairTrabajoParte;
import blender.distributed.Servidor.Trabajo.Trabajo;
import blender.distributed.Servidor.Trabajo.TrabajoStatus;
import blender.distributed.Servidor.Worker.IWorkerAction;
import blender.distributed.Servidor.Worker.WorkerAction;
import blender.distributed.SharedTools.DirectoryTools;
import com.google.gson.Gson;
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
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Thread.sleep;

public class Servidor {
	//General settings
	Logger log = LoggerFactory.getLogger(Servidor.class);
	String serverDirectory = System.getProperty("user.dir")+"\\src\\main\\resources\\Servidor\\";
	String singleServerDir;
	String myFTPDirectory;
	private String myIp;
	private Map<String, PairTrabajoParte> listaWorkers = new HashMap<>();
	private ArrayList<Trabajo> listaTrabajos = new ArrayList<>();
	Map<String, LocalTime> workersLastPing = new HashMap<>();

	//RMI
	private int rmiPortForClientes;
	private int rmiPortForWorkers;
	private int rmiPortForGateway;
	Registry registryCli;
	Registry registrySv;
	Registry registryGw;
	private IClientAction remoteCliente;
	private IFTPAction remoteFtpMan;
	private IWorkerAction remoteWorker;
	private IGatewayAction remoteGateway;


	//Ftp Related
	private int ftpPort;
	ServerFtp ftp;
	

	
	public Servidor() {
		MDC.put("log.name", this.getClass().getSimpleName());
		readConfigFile();
		runFTPServer();
		runRMIServer(this.rmiPortForClientes, this.rmiPortForWorkers, this.rmiPortForGateway);
		while(true) {
			try {
				listaWorkers.forEach((workerName, parTrabajoParte) -> {
				//Checkeo si se cayo un nodo
					int differenceLastKnownPing = (int) Duration.between(workersLastPing.get(workerName), LocalTime.now()).getSeconds();
					if(differenceLastKnownPing > 7) {
						synchronized (listaWorkers) {
							parTrabajoParte.parte().setStatus(TrabajoStatus.TO_DO);
							listaWorkers.remove(workerName);
							log.error("Eliminando al nodo " + workerName + ". Motivo time-out de " + differenceLastKnownPing + " segundos.");
						}
					}
				});
				try {
					sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}catch(Exception e){

			}
		}
	}

	private void runRMIServer(int rmiPortForClientes, int rmiPortForWorkers, int rmiPortForGateway) {
		try {
			System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true"); // renegotiation process is disabled by default.. Without this can't run two clients rmi on same machine like worker and client.
			createSingleServerDir(rmiPortForWorkers);
			log.info("Levantando servidor RMI en puertos " + rmiPortForClientes + "(Clientes) y " + rmiPortForWorkers + "(Workers)");
			registryCli = LocateRegistry.createRegistry(rmiPortForClientes);
			registrySv = LocateRegistry.createRegistry(rmiPortForWorkers);
			registryGw = LocateRegistry.createRegistry(rmiPortForGateway);

			remoteFtpMan = (IFTPAction) UnicastRemoteObject.exportObject(new FTPAction(this.ftpPort, this.ftp),0);
			remoteCliente = (IClientAction) UnicastRemoteObject.exportObject(new ClienteAction(this.listaTrabajos),0);
			remoteWorker = (IWorkerAction) UnicastRemoteObject.exportObject(new WorkerAction(this.listaWorkers, this.listaTrabajos, this.workersLastPing, this.singleServerDir),0);
			remoteGateway = (IGatewayAction) UnicastRemoteObject.exportObject(new GatewayAction(),0);

			registrySv.rebind("ftpAction", remoteFtpMan);
			registryCli.rebind("clientAction", remoteCliente);
			registrySv.rebind("workerAction", remoteWorker);
			registryGw.rebind("gatewayAction", remoteGateway);

			log.info("Servidor RMI:");
			log.info("\t -> Para Clientes: " + registryCli.toString());
			log.info("\t -> Para Workers: " + registrySv.toString());
			log.info("\t -> Para Gateway: " + registryGw.toString());
		} catch (RemoteException e) {
			log.error("Error: Puertos " + rmiPortForClientes + "(Clientes) y " + rmiPortForWorkers + "(Workers) en uso.");
			runRMIServer(++rmiPortForClientes, ++rmiPortForWorkers, ++rmiPortForGateway);
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
			this.rmiPortForGateway = Integer.valueOf(rmi.get("initialPortForGateway").toString());

			Map ftp = (Map) config.get("ftp");
			this.ftpPort = Integer.valueOf(ftp.get("port").toString());
			this.myFTPDirectory = this.serverDirectory + ftp.get("directory").toString();
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

	public static void main(String[] args) {
		new Servidor();
	}
}
