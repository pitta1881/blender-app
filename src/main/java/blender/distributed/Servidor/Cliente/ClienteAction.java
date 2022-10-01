package blender.distributed.Servidor.Cliente;

import blender.distributed.Servidor.ThreadServer;
import blender.distributed.Servidor.Trabajo.Trabajo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ClienteAction implements IClientAction {
	Logger log = LoggerFactory.getLogger(ClienteAction.class);
	ArrayList<Trabajo> listaTrabajos;

	public ClienteAction(ArrayList<Trabajo> listaTrabajos) {
		this.listaTrabajos = listaTrabajos;
		MDC.put("log.name", ClienteAction.class.getSimpleName());
	}

	@Override
	public String helloServer(String clientIp, String myHostName) throws RemoteException {
		log.info("Se conecto el cliente " + clientIp + " - " + myHostName);
		return "OK";
	}

	@Override
	public byte[] renderRequest(Trabajo work) throws RemoteException {
		listaTrabajos.add(work);
		CountDownLatch latch = new CountDownLatch(1);
		List<ThreadServer> serverThread = new ArrayList<>();
		serverThread.add(new ThreadServer(latch, work));
		Executor executor = Executors.newFixedThreadPool(serverThread.size());
		for(final ThreadServer wt : serverThread) {
			executor.execute(wt);
		}
		try {
			latch.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		listaTrabajos.remove(work);
		return work.getZipWithRenderedImages();
	}
}
