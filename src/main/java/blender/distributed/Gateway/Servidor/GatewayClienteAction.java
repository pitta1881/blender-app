package blender.distributed.Gateway.Servidor;

import blender.distributed.Enums.ENodo;
import blender.distributed.Records.RServidor;
import blender.distributed.Servidor.Cliente.IClienteAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Random;

import static blender.distributed.SharedTools.Tools.manageGatewayServidorFall;

public class GatewayClienteAction implements IClienteAction {
	Logger log = LoggerFactory.getLogger(GatewayClienteAction.class);
	List<RServidor> listaServidores;
	public GatewayClienteAction(List<RServidor> listaServidores) {
		MDC.put("log.name", GatewayClienteAction.class.getSimpleName());
		this.listaServidores = listaServidores;
	}

	@Override
	public String renderRequest(byte[] blendFile, String blendName, int startFrame, int endFrame) {
		try {
			return connectRandomServidorRMIForCliente().renderRequest(blendFile, blendName, startFrame, endFrame);
		} catch (NullPointerException | RemoteException e) {
			return renderRequest(blendFile, blendName, startFrame, endFrame);
		}
	}

	@Override
	public String getTrabajo(String uuid) {
		try {
			return connectRandomServidorRMIForCliente().getTrabajo(uuid);
		} catch (NullPointerException | RemoteException e) {
			return getTrabajo(uuid);
		}
	}

	private IClienteAction connectRandomServidorRMIForCliente() {
		IClienteAction stubServidor = null;
		if(this.listaServidores.size() > 0) {
			Random rand = new Random();
			int nRandomServidor = rand.nextInt(this.listaServidores.size());
			String ip = this.listaServidores.get(nRandomServidor).ip();
			int port = this.listaServidores.get(nRandomServidor).rmiPortForClientes();
			try {
				Registry clienteRMI = LocateRegistry.getRegistry(ip, port);
				stubServidor = (IClienteAction) clienteRMI.lookup("clienteAction");
				return stubServidor;
			} catch (RemoteException | NotBoundException e) {
				log.error("Error: " + e.getMessage());
				manageGatewayServidorFall(ENodo.SERVIDOR, ip, port);
				return connectRandomServidorRMIForCliente();
			}
		} else {
			log.error("No hay ningun servidor disponible.");
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
			}
			return connectRandomServidorRMIForCliente();
		}
	}
}
