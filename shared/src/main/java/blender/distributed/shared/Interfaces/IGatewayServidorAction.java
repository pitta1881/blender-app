package blender.distributed.gateway.Servidor;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IGatewayServidorAction extends Remote{
	String helloGatewayFromServidor(String publicIp, int rmiPortForClientes, int rmiPortForWorkers) throws RemoteException;
	void helloServerFromWorker(String workerName, String recordWorkerJson) throws RemoteException;
	void pingAliveFromServidor(String uuidServidor, String publicIp, int rmiPortForClientes, int rmiPortForWorkers) throws RemoteException;
	String getWorker(String workerName) throws RemoteException;
	void setWorker(String workerName, String recordWorkerJson) throws RemoteException;
	String getTrabajo(String uuidTrabajo) throws RemoteException;
	void setTrabajo(String uuidTrabajo, String recordTrabajoJson) throws RemoteException;
	void delTrabajo(String uuidTrabajo) throws RemoteException;
	String getParte(String uuidParte) throws RemoteException;
	List<String> getAllPartes() throws RemoteException;
	void setParte(String uuidParte, String recordParteJson) throws RemoteException;
	void delParte(String uuidParte) throws RemoteException;
	void storeBlendFile(String gStorageBlendName, byte[] blendFile) throws RemoteException;
	byte[] getBlendFile(String gStorageBlendName) throws RemoteException;
	void deleteBlendFile(String gStorageBlendName) throws RemoteException;
	void storePartZipFile(String gStorageZipName, byte[] zipFile) throws RemoteException;
	byte[] getPartZipFile(String gStorageZipName) throws RemoteException;
	void deletePartZipFile(String gStorageZipName) throws RemoteException;
	void storeFinalZipFile(String gStorageZipName, byte[] zipFile) throws RemoteException;
}

