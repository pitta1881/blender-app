package blender.distributed.Gateway.Servidor;

import blender.distributed.Servidor.FTP.IFTPAction;
import blender.distributed.Servidor.Worker.IWorkerAction;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IServidorWorkerAction extends IWorkerAction, IFTPAction, Remote{
    void setPrimaryServerPort(int port) throws RemoteException;
	String helloGateway() throws RemoteException;
}
