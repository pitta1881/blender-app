package blender.distributed.Servidor;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IFTPManager extends Remote{
	
	int startFTPServer() throws RemoteException;
	boolean stopFTPServer() throws RemoteException;
	int getFTPPort() throws RemoteException;
	boolean isFTPStopped() throws RemoteException;
	boolean resumeFTPServer() throws RemoteException;
}
