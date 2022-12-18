package blender.distributed.Gateway.Servidor;

import blender.distributed.Enums.EServicio;
import blender.distributed.Gateway.Tools;
import blender.distributed.Records.RServidor;
import blender.distributed.Servidor.Cliente.IClienteAction;
import org.slf4j.Logger;


import java.rmi.RemoteException;
import java.util.List;

public class GatewayClienteAction implements IClienteAction {
	Logger log;
	List<RServidor> listaServidores;
	public GatewayClienteAction(List<RServidor> listaServidores, Logger log) {
		this.listaServidores = listaServidores;
		this.log = log;
	}

	@Override
	public String renderRequest(byte[] blendFile, String blendName, int startFrame, int endFrame) {
		try {
			return Tools.<IClienteAction>connectRandomServidorRMI(this.listaServidores, EServicio.CLIENTE_ACTION, this.log).renderRequest(blendFile, blendName, startFrame, endFrame);
		} catch (NullPointerException | RemoteException e) {
			return renderRequest(blendFile, blendName, startFrame, endFrame);
		}
	}

	@Override
	public String getTrabajo(String uuid) {
		try {
			return Tools.<IClienteAction>connectRandomServidorRMI(this.listaServidores, EServicio.CLIENTE_ACTION, this.log).getTrabajo(uuid);
		} catch (NullPointerException | RemoteException e) {
			return getTrabajo(uuid);
		}
	}

}
