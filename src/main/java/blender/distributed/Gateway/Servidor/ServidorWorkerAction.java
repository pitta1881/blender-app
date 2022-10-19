package blender.distributed.Gateway.Servidor;

import blender.distributed.Servidor.FTP.IFTPAction;
import blender.distributed.Servidor.Trabajo.PairTrabajoParte;
import blender.distributed.Servidor.Worker.IWorkerAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServidorWorkerAction implements IServidorWorkerAction {
	Logger log = LoggerFactory.getLogger(ServidorWorkerAction.class);
	String ip;
	int initialPort;
	IWorkerAction stubWorker = null;
	IFTPAction stubFtp;
	int max_servers;

	public ServidorWorkerAction(String ip, int initialPort, int max_servers) {
		MDC.put("log.name", ServidorWorkerAction.class.getSimpleName());
		this.ip = ip;
		this.initialPort = initialPort + 1;
		this.max_servers = max_servers;
	}

	@Override
	public String helloGateway() {
		return "OK";
	}

	@Override
	public void pingAlive(String workerName) {
		try {
			this.stubWorker.pingAlive(workerName);
		} catch (RemoteException | NullPointerException e) {
			connectRMI(this.ip, initialPort);
			pingAlive(workerName);
		}
	}

	@Override
	public PairTrabajoParte giveWorkToDo(String workerName) {
		try {
			return this.stubWorker.giveWorkToDo(workerName);
		} catch (RemoteException | NullPointerException e) {
			connectRMI(this.ip, initialPort);
			return giveWorkToDo(workerName);
		}
	}

	@Override
	public void setTrabajoParteStatusDone(String workerName, String trabajoId, int nParte, byte[] zipWithRenderedImages) {
		try {
			this.stubWorker.setTrabajoParteStatusDone(workerName, trabajoId, nParte, zipWithRenderedImages);
		} catch (RemoteException | NullPointerException e) {
			connectRMI(this.ip, initialPort);
			setTrabajoParteStatusDone(workerName, trabajoId, nParte, zipWithRenderedImages);
		}
	}

	@Override
	public int startFTPServer() {
		try {
			return this.stubFtp.startFTPServer();
		} catch (RemoteException | NullPointerException e) {
			connectRMI(this.ip, initialPort);
			return startFTPServer();
		}
	}

	@Override
	public boolean stopFTPServer() {
		try {
			return this.stubFtp.stopFTPServer();
		} catch (RemoteException | NullPointerException e) {
			connectRMI(this.ip, initialPort);
			return stopFTPServer();
		}
	}

	@Override
	public int getFTPPort() {
		try {
			return this.stubFtp.getFTPPort();
		} catch (RemoteException | NullPointerException e) {
			connectRMI(this.ip, initialPort);
			return getFTPPort();
		}
	}

	@Override
	public boolean isFTPStopped() {
		try {
			return this.stubFtp.isFTPStopped();
		} catch (RemoteException | NullPointerException e) {
			connectRMI(this.ip, initialPort);
			return isFTPStopped();
		}
	}

	@Override
	public boolean resumeFTPServer() {
		try {
			return this.stubFtp.resumeFTPServer();
		} catch (RemoteException | NullPointerException e) {
			connectRMI(this.ip, initialPort);
			return resumeFTPServer();
		}
	}

	private void connectRMI(String ip, int port) {
		this.stubWorker = null;
		if(port == (this.initialPort+this.max_servers))
			port = this.initialPort;
		try {
			Registry workerRMI = LocateRegistry.getRegistry(ip, port);
			this.stubWorker = (IWorkerAction) workerRMI.lookup("workerAction");
			this.stubFtp = (IFTPAction) workerRMI.lookup("ftpAction");
			log.info("Conectado al Servidor " + ip + ":" + port);
		} catch (RemoteException | NotBoundException e) {
			log.error("Error al conectar con el Servidor " + ip + ":" + port);
			connectRMI(ip, port + 1);
		}
	}
}
