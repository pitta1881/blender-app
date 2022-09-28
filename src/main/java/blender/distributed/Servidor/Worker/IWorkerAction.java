package blender.distributed.Servidor.Worker;

import blender.distributed.Servidor.Trabajo.Trabajo;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IWorkerAction extends Remote{
	void helloServer(String workerName) throws RemoteException;
	void checkStatus() throws RemoteException;
	Trabajo giveWorkToDo(String worker) throws RemoteException;
	void setTrabajoStatusDone(String id, byte[] zipWithRenderedImages) throws RemoteException;

}
