package blender.distributed.Gateway.Servidor;

import blender.distributed.Gateway.PairParteLastping;
import blender.distributed.Servidor.Trabajo.Trabajo;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IGatewayServidorAction extends Remote{

	String helloGateway(int rmiPortForClientes, int rmiPortForWorkers) throws RemoteException;
	PairParteLastping getWorker(String workerName) throws RemoteException;
	void setWorker(String workerName, PairParteLastping workerRecord) throws RemoteException;
	void delWorker(String workerName) throws RemoteException;
	Trabajo getTrabajo(String workId) throws RemoteException;
	List<Object> getAllTrabajos() throws RemoteException;
	void setTrabajo(Trabajo work) throws RemoteException;
	void delTrabajo(String workId) throws RemoteException;

}
