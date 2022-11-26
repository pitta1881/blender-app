package blender.distributed.Servidor.Worker;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IWorkerAction extends Remote {
	void pingAlive(String workerName) throws RemoteException;
	String getWorkToDo(String workerName) throws RemoteException;
	void setParteDone(String workerName, String uuidParte, byte[] zipParteWithRenderedImages) throws RemoteException;

}
