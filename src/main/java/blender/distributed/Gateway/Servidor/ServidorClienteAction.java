package blender.distributed.Gateway.Servidor;

import blender.distributed.Servidor.Cliente.ClienteAction;
import blender.distributed.Servidor.Cliente.IClientAction;
import blender.distributed.Servidor.Trabajo.Trabajo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServidorClienteAction implements IServidorClientAction {
	Logger log = LoggerFactory.getLogger(ClienteAction.class);
	String ip;
	IClientAction stubCliente = null;
	int primaryServerPort;

	public ServidorClienteAction(String ip) {
		MDC.put("log.name", ServidorClienteAction.class.getSimpleName());
		this.ip = ip;
	}

	@Override
	public void setPrimaryServerPort(int port){
		this.primaryServerPort = port;
	}

	@Override
	public String helloGateway() {
		return "OK";
	}

	@Override
	public byte[] renderRequest(Trabajo work) {
		try {
			return this.stubCliente.renderRequest(work);
		} catch (RemoteException | NullPointerException e) {
			e.printStackTrace();
			connectRMI();
			return renderRequest(work);
		}
	}

	private void connectRMI() {
		this.stubCliente = null;
		try {
			Thread.sleep(1000);
			Registry clienteRMI = LocateRegistry.getRegistry(this.ip, this.primaryServerPort);
			this.stubCliente = (IClientAction) clienteRMI.lookup("clientAction");
			log.info("Conectado al Servidor " + this.ip + ":" + this.primaryServerPort);
		} catch (RemoteException | NotBoundException | InterruptedException e) {
			log.error("Error al conectar con el Servidor " + this.ip + ":" + this.primaryServerPort);
			connectRMI();
		}
	}
}
