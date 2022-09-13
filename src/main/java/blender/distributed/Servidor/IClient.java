package blender.distributed.Servidor;

import blender.distributed.Cliente.Imagen;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface IClient extends Remote{

	public String helloFromClient(String clientIp, String myHostName) throws RemoteException;
	public Imagen renderRequest(Mensaje msg) throws RemoteException;

}
