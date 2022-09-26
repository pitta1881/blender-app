package blender.distributed.Servidor;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface IClientAction extends Remote{

	String helloServer(String clientIp, String myHostName) throws RemoteException;
	byte[] renderRequest(Trabajo work) throws RemoteException;

}
