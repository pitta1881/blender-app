package blender.distributed.Servidor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.rmi.RemoteException;
import java.util.ArrayList;

public class ClienteAction implements IClientAction {
	Logger log = LoggerFactory.getLogger(ClienteAction.class);
	ArrayList<Trabajo> listaTrabajos;
	byte[] zipWithRenderedImages = null;


	public ClienteAction(ArrayList<Trabajo> listaTrabajos) {
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
	public byte[] renderRequest(Trabajo work) throws RemoteException {
		listaTrabajos.add(work);
		ThreadServer thServer = new ThreadServer(work);
		Thread th = new Thread(thServer);
		th.start();
		while(work.getStatus() != 3) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		log.info("Imágenes renderizadas");
		th.interrupt();
		listaTrabajos.remove(work);
		return work.getZipWithRenderedImages();
	}
}
