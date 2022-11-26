package blender.distributed.Gateway.Servidor;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IGatewayServidorAction extends Remote{
	String helloGatewayFromServidor(int rmiPortForClientes, int rmiPortForWorkers) throws RemoteException;
	void pingAliveFromServidor(String uuidServidor, int rmiPortForClientes, int rmiPortForWorkers) throws RemoteException;
	String getWorker(String workerName) throws RemoteException;
	String getAllWorkers() throws RemoteException;
	void setWorker(String workerName, String recordWorkerJson) throws RemoteException;
	void delWorker(String workerName) throws RemoteException;
	String getTrabajo(String uuidTrabajo) throws RemoteException;
	String getAllTrabajos() throws RemoteException;
	void setTrabajo(String uuidTrabajo, String recordTrabajoJson) throws RemoteException;
	void delTrabajo(String uuidTrabajo) throws RemoteException;
	String getParte(String uuidParte) throws RemoteException;
	List<String> getAllPartes() throws RemoteException;
	void setParte(String uuidParte, String recordParteJson) throws RemoteException;
	void delParte(String uuidParte) throws RemoteException;
	String storeBlendFile(String blendName, byte[] blendFile) throws RemoteException;
	String storeZipFile(String zipName, byte[] zipFile) throws RemoteException;
	byte[] getZipFile(String uuidParte) throws RemoteException;
}

