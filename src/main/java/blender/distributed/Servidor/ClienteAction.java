package blender.distributed.Servidor;

import blender.distributed.Cliente.Imagen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.rmi.RemoteException;
import java.util.ArrayList;

public class ClienteAction implements IClientAction {
	Logger log = LoggerFactory.getLogger(ClienteAction.class);
	ArrayList<Mensaje> listaTrabajos;


	public ClienteAction(ArrayList<Mensaje> listaTrabajos) {
		this.listaTrabajos = listaTrabajos;
		MDC.put("log.name", ClienteAction.class.getSimpleName().toString())
		;
	}

	@Override
	public String helloServer(String clientIp, String myHostName) throws RemoteException {
		log.info("Se conecto el cliente " + clientIp + " - " + myHostName);
		return "OK";
	}

	@Override
	public Imagen renderRequest(Mensaje msg) throws RemoteException {
		listaTrabajos.add(msg);
		ThreadServer thServer = new ThreadServer(msg, this.listaTrabajos);
		Thread th = new Thread(thServer);
		th.start();
		while(thServer.getRenderedImage() == null) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		log.info("Imagen renderizada");
		th.interrupt();
		return new Imagen(thServer.getRenderedImage());
	}
}
