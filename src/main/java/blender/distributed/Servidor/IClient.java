package blender.distributed.Servidor;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface IClient extends Remote{

	public String helloFromClient(String clientIp)throws RemoteException;
}
