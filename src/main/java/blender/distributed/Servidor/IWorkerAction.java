package blender.distributed.Servidor;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IWorkerAction extends Remote{
	void helloServer(String worker) throws RemoteException;
	void checkStatus() throws RemoteException;
}
