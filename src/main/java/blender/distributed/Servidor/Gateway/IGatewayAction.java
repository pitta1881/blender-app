package blender.distributed.Servidor.Gateway;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface IGatewayAction extends Remote{
	void simplePing() throws RemoteException;

}
