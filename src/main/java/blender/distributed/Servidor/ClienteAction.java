package blender.distributed.Servidor;

import blender.distributed.Cliente.Imagen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.rmi.RemoteException;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

public class ClienteAction implements IClientAction {
	Map<String,LocalTime> workersLastPing = new HashMap<String,LocalTime>();
	Logger log = LoggerFactory.getLogger(ClienteAction.class);

	public ClienteAction() {
		MDC.put("log.name", ClienteAction.class.getSimpleName().toString());
	}

	@Override
	public String helloServer(String clientIp, String myHostName) throws RemoteException {
		log.info("Se conecto el cliente " + clientIp + " - " + myHostName);
		return "OK";
	}

	@Override
	public Imagen renderRequest(Mensaje msg) throws RemoteException {
		return null;
	}
}
