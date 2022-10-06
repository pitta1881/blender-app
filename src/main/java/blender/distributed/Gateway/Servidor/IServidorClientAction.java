package blender.distributed.Gateway.Servidor;

import blender.distributed.Servidor.Cliente.IClientAction;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface IServidorClientAction extends IClientAction, Remote{
	String helloGateway() throws RemoteException;

}
