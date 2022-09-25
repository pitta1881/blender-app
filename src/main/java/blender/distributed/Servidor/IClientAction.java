package blender.distributed.Servidor;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface IClientAction extends Remote{

	public String helloServer(String clientIp, String myHostName) throws RemoteException;
	public byte[] renderRequest(Mensaje msg) throws RemoteException;

}
