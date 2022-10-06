package blender.distributed.Gateway;

import blender.distributed.Gateway.Servidor.IServidorClientAction;
import blender.distributed.Gateway.Servidor.IServidorWorkerAction;
import blender.distributed.Gateway.Servidor.ServidorClienteAction;
import blender.distributed.Gateway.Servidor.ServidorWorkerAction;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

public class Gateway {
	//General settings
	Logger log = LoggerFactory.getLogger(Gateway.class);
	String gatewayDirectory = System.getProperty("user.dir")+"\\src\\main\\resources\\Gateway\\";
	private String myIp;
	private int max_servers;

	//RMI
	private int rmiPortForClientes;
	private int rmiPortForWorkers;
	Registry registryCli;
	Registry registrySv;
	private IServidorClientAction remoteCliente;
	private IServidorWorkerAction remoteWorker;

	public Gateway() {
		MDC.put("log.name", this.getClass().getSimpleName());
		readConfigFile();
		try {
			runRMIServer();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private void runRMIServer() throws RemoteException {
		System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true"); // renegotiation process is disabled by default.. Without this can't run two clients rmi on same machine like worker and client.
		log.info("Levantando gateway RMI...");
		registryCli = LocateRegistry.createRegistry(this.rmiPortForClientes);
		registrySv = LocateRegistry.createRegistry(this.rmiPortForWorkers);

		remoteCliente = (IServidorClientAction) UnicastRemoteObject.exportObject(new ServidorClienteAction(this.myIp, this.rmiPortForClientes, max_servers),0);
		remoteWorker = (IServidorWorkerAction) UnicastRemoteObject.exportObject(new ServidorWorkerAction(this.myIp, this.rmiPortForWorkers, max_servers),0);

		registryCli.rebind("clientAction", remoteCliente);
		registrySv.rebind("workerAction", remoteWorker);
		log.info("Gateway RMI (MAX SERVERS: " + this.max_servers + ")");
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
			this.max_servers = Integer.valueOf(server.get("max_servers").toString());

			Map rmi = (Map) config.get("rmi");
			this.rmiPortForClientes = Integer.valueOf(rmi.get("portForClientes").toString());
			this.rmiPortForWorkers = Integer.valueOf(rmi.get("portForWorkers").toString());

		} catch (IOException e) {
		}
	}

	public static void main(String[] args) {
		new Gateway();
	}
}
