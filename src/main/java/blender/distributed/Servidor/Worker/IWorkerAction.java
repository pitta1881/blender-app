package blender.distributed.Servidor.Worker;

import blender.distributed.Servidor.Trabajo.PairTrabajoParte;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IWorkerAction extends Remote{
	void pingAlive(String workerName) throws RemoteException;
	PairTrabajoParte giveWorkToDo(String workerName) throws RemoteException;
	void setTrabajoParteStatusDone(String workerName, String trabajoId, int parte, byte[] zipWithRenderedImages) throws RemoteException;

}
