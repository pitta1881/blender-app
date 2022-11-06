package blender.distributed.Gateway.Servidor;

import blender.distributed.Servidor.Cliente.IClientAction;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface IServidorClientAction extends IClientAction, Remote{
	void setPrimaryServerPort(int port) throws RemoteException;
	String helloGateway() throws RemoteException;

}
