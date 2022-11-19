package blender.distributed.Gateway.Servidor;

import blender.distributed.Gateway.PairIpPortCPortW;
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
import java.util.ArrayList;
import java.util.Random;

public class GatewayClienteAction implements IGatewayClientAction {
	Logger log = LoggerFactory.getLogger(ClienteAction.class);
	ArrayList<PairIpPortCPortW> listaServidores;
	public GatewayClienteAction(ArrayList<PairIpPortCPortW> listaServidores) {
		MDC.put("log.name", GatewayClienteAction.class.getSimpleName());
		this.listaServidores = listaServidores;
	}

	@Override
	public String helloGateway() {
		return "OK";
	}

	@Override
	public byte[] renderRequest(Trabajo work) {
		try {
			return this.connectRandomRMI().renderRequest(work);
		} catch (RemoteException | NullPointerException e) {
			return renderRequest(work);
		}
	}

	private IClientAction connectRandomRMI() {
		IClientAction stubCliente = null;
		if(this.listaServidores.size() > 0) {
			synchronized (this.listaServidores) {
				Random rand = new Random();
				int nRandomServer = rand.nextInt(this.listaServidores.size());
				String ip = this.listaServidores.get(nRandomServer).ip();
				int port = this.listaServidores.get(nRandomServer).rmiPortForClientes();
				try {
					Thread.sleep(1000);
					Registry clienteRMI = LocateRegistry.getRegistry(ip, port);
					stubCliente = (IClientAction) clienteRMI.lookup("clientAction");
					return stubCliente;
				} catch (RemoteException | NotBoundException | InterruptedException e) {
					log.error("Error al conectar con el Servidor " + ip + ":" + port);
					return connectRandomRMI();
				}
			}
		} else {
			log.error("No hay ningun servidor disponible.");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			return connectRandomRMI();
		}
	}
}
