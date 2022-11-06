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
	IWorkerAction stubWorker = null;
	IFTPAction stubFtp;
	int primaryServerPort;

	public ServidorWorkerAction(String ip) {
		MDC.put("log.name", ServidorWorkerAction.class.getSimpleName());
		this.ip = ip;
	}

	@Override
	public String helloGateway() {
		return "OK";
	}

	@Override
	public void setPrimaryServerPort(int port){
		this.primaryServerPort = port;
	}
	@Override
	public void pingAlive(String workerName) {
		try {
			this.stubWorker.pingAlive(workerName);
		} catch (RemoteException | NullPointerException e) {
			e.printStackTrace();
			connectRMI();
			pingAlive(workerName);
		}
	}

	@Override
	public PairTrabajoParte giveWorkToDo(String workerName) {
		try {
			return this.stubWorker.giveWorkToDo(workerName);
		} catch (RemoteException | NullPointerException e) {
			connectRMI();
			return giveWorkToDo(workerName);
		}
	}

	@Override
	public void setTrabajoParteStatusDone(String workerName, String trabajoId, int nParte, byte[] zipWithRenderedImages) {
		try {
			this.stubWorker.setTrabajoParteStatusDone(workerName, trabajoId, nParte, zipWithRenderedImages);
		} catch (RemoteException | NullPointerException e) {
			connectRMI();
			setTrabajoParteStatusDone(workerName, trabajoId, nParte, zipWithRenderedImages);
		}
	}

	@Override
	public int startFTPServer() {
		try {
			return this.stubFtp.startFTPServer();
		} catch (RemoteException | NullPointerException e) {
			connectRMI();
			return startFTPServer();
		}
	}

	@Override
	public boolean stopFTPServer() {
		try {
			return this.stubFtp.stopFTPServer();
		} catch (RemoteException | NullPointerException e) {
			connectRMI();
			return stopFTPServer();
		}
	}

	@Override
	public int getFTPPort() {
		try {
			return this.stubFtp.getFTPPort();
		} catch (RemoteException | NullPointerException e) {
			connectRMI();
			return getFTPPort();
		}
	}

	@Override
	public boolean isFTPStopped() {
		try {
			return this.stubFtp.isFTPStopped();
		} catch (RemoteException | NullPointerException e) {
			connectRMI();
			return isFTPStopped();
		}
	}

	@Override
	public boolean resumeFTPServer() {
		try {
			return this.stubFtp.resumeFTPServer();
		} catch (RemoteException | NullPointerException e) {
			connectRMI();
			return resumeFTPServer();
		}
	}

	private void connectRMI() {
		this.stubWorker = null;
		try {
			Thread.sleep(1000);
			Registry workerRMI = LocateRegistry.getRegistry(this.ip, this.primaryServerPort);
			this.stubWorker = (IWorkerAction) workerRMI.lookup("workerAction");
			this.stubFtp = (IFTPAction) workerRMI.lookup("ftpAction");
			log.info("Conectado al Servidor " + this.ip + ":" + this.primaryServerPort);
		} catch (RemoteException | NotBoundException | InterruptedException e) {
			log.error("Error al conectar con el Servidor " + this.ip + ":" + this.primaryServerPort);
			connectRMI();
		}
	}
}
