package blender.distributed.Servidor;

import blender.distributed.Servidor.helpers.ServerFtp;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Servidor {
	//General settings
	Logger log = LoggerFactory.getLogger(Servidor.class);
	String serverDirectory = System.getProperty("user.dir")+"\\src\\main\\resources\\Servidor\\";
	URL serverConfigFile = this.getClass().getClassLoader().getResource("Servidor\\config.json");
	String myFTPDirectory;
	private String myIp;
	private String backupIp;
	private ArrayList<String> listaWorkers = new ArrayList<String>();
	private ArrayList<String> listaTrabajos = new ArrayList<String>();
	Map<String, LocalTime> workersLastPing = new HashMap<String,LocalTime>();

	//RMI
	private int rmiPortCli;
	private int rmiPortSv;
	Registry registryCli;
	Registry registrySv;
	private IClient remoteClient;
	private IFTPManager remoteFtpMan;
	private IWorkerAction remoteWorker;

	//Ftp Related
	private int ftpPort;
	ServerFtp ftp;
	

	
	public Servidor() {
		MDC.put("log.name", this.getClass().getSimpleName().toString());
		readConfigFile();
		initialConfig();
		try {
			runRMIServer();
			while(true) {
				try {
					//Checkeo si se cayo un nodo
					for(String str : listaWorkers) {
						if((int) Duration.between(workersLastPing.get(str), LocalTime.now()).getSeconds() > 70) {
							synchronized (listaWorkers) {
								listaWorkers.remove(str);
								log.error("Eliminando al nodo "+str+". Motivo time-out de "+(int)Duration.between(workersLastPing.get(str), LocalTime.now()).getSeconds()+" segundos.");
							}
						}
					}
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}catch(Exception e){

				}
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private void runRMIServer() throws RemoteException {
		System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true"); // renegotiation process is disabled by default.. Without this can't run two clients rmi on same machine like worker and client.
		log.info("Levantando servidor RMI...");
		registryCli = LocateRegistry.createRegistry(this.rmiPortCli);
		registrySv = LocateRegistry.createRegistry(this.rmiPortSv);

		remoteFtpMan = (IFTPManager) UnicastRemoteObject.exportObject(new FTPManager(this.ftpPort, this.ftp),0);
		//remoteClient = (IClient) UnicastRemoteObject.exportObject(this,0);
		remoteWorker = (IWorkerAction) UnicastRemoteObject.exportObject(new WorkerAction(this.listaWorkers, this.listaTrabajos, this.workersLastPing),0);

		registrySv.rebind("Acciones", remoteFtpMan);
		//registryCli.rebind("client", remoteClient);
		registrySv.rebind("server", remoteWorker);
		log.info("Servidor RMI{");
		//log.info("\t Client:"+registryCli.toString());
		log.info("\t Server:"+registrySv.toString()+"\n\t\t\t}");
	}
	
	private void readConfigFile() {
		Gson gson = new Gson();
		Map config;
		try {
			config = gson.fromJson(new FileReader(serverDirectory+"config.json"), Map.class);
			
			Map data = (Map) config.get("server");
			this.myIp = data.get("ip").toString();
			this.backupIp =  data.get("ipBak").toString();

			data = (Map) config.get("rmi");
			this.rmiPortCli = Integer.valueOf(data.get("portCli").toString());
			this.rmiPortSv = Integer.valueOf(data.get("portSv").toString());

			data = (Map) config.get("ftp");
			this.ftpPort = Integer.valueOf(data.get("port").toString());
			this.myFTPDirectory = this.serverDirectory + data.get("directory").toString();
		} catch (IOException e) {
		}
	}

	private void initialConfig() {
		//FTP RELATED
		this.ftp = new ServerFtp(this.ftpPort, this.myFTPDirectory);
		log.info("FTP Configurado correctamente. Listo para usar en puerto:"+this.ftpPort+". Compartiendo carpeta: "+this.myFTPDirectory);
	}


	public static void main(String[] args) {
		new Servidor();
	}
}
