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
	int initialPort;
	IClientAction stubCliente = null;
	int max_servers;

	public ServidorClienteAction(String ip, int initialPort, int max_servers) {
		MDC.put("log.name", ServidorClienteAction.class.getSimpleName());
		this.ip = ip;
		this.initialPort = initialPort + 1;
		this.max_servers = max_servers;
	}

	@Override
	public String helloGateway() {
		return "OK";
	}

	@Override
	public String helloServer(String clientIp, String clienteHostName)  {
		try {
			return this.stubCliente.helloServer(clientIp, clienteHostName);
		} catch (RemoteException | NullPointerException e) {
			connectRMI(this.ip, initialPort);
			return helloServer(clientIp, clienteHostName);
		}
	}

	@Override
	public byte[] renderRequest(Trabajo work) {
		try {
			return this.stubCliente.renderRequest(work);
		} catch (RemoteException | NullPointerException e) {
			connectRMI(this.ip, initialPort);
			return renderRequest(work);
		}
	}

	private void connectRMI(String ip, int port) {
		this.stubCliente = null;
		if(port == (this.initialPort+this.max_servers))
			port = this.initialPort;
		try {
			Registry workerRMI = LocateRegistry.getRegistry(ip, port);
			this.stubCliente = (IClientAction) workerRMI.lookup("clientAction");
			log.info("Conectado al Servidor " + ip + ":" + port);
		} catch (RemoteException | NotBoundException e) {
			log.error("Error al conectar con el Servidor " + ip + ":" + port);
			connectRMI(ip, port + 1);
		}
	}
}
