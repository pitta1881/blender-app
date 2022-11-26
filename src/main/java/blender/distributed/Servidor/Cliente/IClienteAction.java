package blender.distributed.Servidor.Cliente;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface IClienteAction extends Remote{
	String renderRequest(byte[] blendFile, String blendName, int startFrame, int endFrame) throws RemoteException;
	String getTrabajo(String uuid) throws RemoteException;
}
