package blender.distributed.Servidor.Gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.rmi.RemoteException;

public class GatewayAction implements IGatewayAction {
	Logger log = LoggerFactory.getLogger(GatewayAction.class);

	public GatewayAction() {
		MDC.put("log.name", GatewayAction.class.getSimpleName());
	}

	@Override
	public void simplePing() throws RemoteException {}
}
