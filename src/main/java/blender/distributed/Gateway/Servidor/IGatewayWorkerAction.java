package blender.distributed.Gateway.Servidor;

import blender.distributed.Servidor.FTP.IFTPAction;
import blender.distributed.Servidor.Worker.IWorkerAction;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IGatewayWorkerAction extends IWorkerAction, IFTPAction, Remote{
	String helloGateway() throws RemoteException;
}
