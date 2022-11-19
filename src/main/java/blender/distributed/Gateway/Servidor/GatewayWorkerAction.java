package blender.distributed.Gateway.Servidor;

import blender.distributed.Gateway.PairIpPortCPortW;
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
import java.util.ArrayList;
import java.util.Random;

public class GatewayWorkerAction implements IGatewayWorkerAction {
	Logger log = LoggerFactory.getLogger(GatewayWorkerAction.class);
	ArrayList<PairIpPortCPortW> listaServidores;

	public GatewayWorkerAction(ArrayList<PairIpPortCPortW> listaServidores) {
		MDC.put("log.name", GatewayWorkerAction.class.getSimpleName());
		this.listaServidores = listaServidores;
	}

	@Override
	public String helloGateway() {
		return "OK";
	}

	@Override
	public void pingAlive(String workerName) {
		try {
			this.connectRandomRMIWorker().pingAlive(workerName);
		} catch (RemoteException | NullPointerException e) {
			pingAlive(workerName);
		}
	}

	@Override
	public PairTrabajoParte giveWorkToDo(String workerName) {
		try {
			return this.connectRandomRMIWorker().giveWorkToDo(workerName);
		} catch (RemoteException | NullPointerException e) {
			return giveWorkToDo(workerName);
		}
	}

	@Override
	public void setTrabajoParteStatusDone(String workerName, String trabajoId, int nParte, byte[] zipWithRenderedImages) {
		try {
			this.connectRandomRMIWorker().setTrabajoParteStatusDone(workerName, trabajoId, nParte, zipWithRenderedImages);
		} catch (RemoteException | NullPointerException e) {
			e.printStackTrace();
			setTrabajoParteStatusDone(workerName, trabajoId, nParte, zipWithRenderedImages);
		}
	}

	@Override
	public int startFTPServer() {
		try {
			return this.connectRandomRMIFtp().startFTPServer();
		} catch (RemoteException | NullPointerException e) {
			return startFTPServer();
		}
	}

	@Override
	public boolean stopFTPServer() {
		try {
			return this.connectRandomRMIFtp().stopFTPServer();
		} catch (RemoteException | NullPointerException e) {
			return stopFTPServer();
		}
	}

	@Override
	public int getFTPPort() {
		try {
			return this.connectRandomRMIFtp().getFTPPort();
		} catch (RemoteException | NullPointerException e) {
			return getFTPPort();
		}
	}

	@Override
	public boolean isFTPStopped() {
		try {
			return this.connectRandomRMIFtp().isFTPStopped();
		} catch (RemoteException | NullPointerException e) {
			return isFTPStopped();
		}
	}

	@Override
	public boolean resumeFTPServer() {
		try {
			return this.connectRandomRMIFtp().resumeFTPServer();
		} catch (RemoteException | NullPointerException e) {
			return resumeFTPServer();
		}
	}

	private IWorkerAction connectRandomRMIWorker() {
		IWorkerAction stubWorker = null;
		if(this.listaServidores.size() > 0) {
			synchronized (this.listaServidores) {
				Random rand = new Random();
				int nRandomServer = rand.nextInt(this.listaServidores.size());
				String ip = this.listaServidores.get(nRandomServer).ip();
				int port = this.listaServidores.get(nRandomServer).rmiPortForWorkers();
				try {
					Thread.sleep(1000);
					Registry workerRMI = LocateRegistry.getRegistry(ip, port);
					stubWorker = (IWorkerAction) workerRMI.lookup("workerAction");
					return stubWorker;
				} catch (RemoteException | NotBoundException | InterruptedException e) {
					log.error("Error al conectar con el Servidor " + ip + ":" + port);
					return connectRandomRMIWorker();
				}
			}
		} else {
			log.error("No hay ningun servidor disponible.");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			return connectRandomRMIWorker();
		}
	}
	private IFTPAction connectRandomRMIFtp() {
		IFTPAction stubFTP = null;
		if(this.listaServidores.size() > 0) {
			synchronized (this.listaServidores) {
				Random rand = new Random();
				int nRandomServer = rand.nextInt(this.listaServidores.size());
				String ip = this.listaServidores.get(nRandomServer).ip();
				int port = this.listaServidores.get(nRandomServer).rmiPortForWorkers();
				try {
					Thread.sleep(1000);
					Registry workerRMI = LocateRegistry.getRegistry(ip, port);
					stubFTP = (IFTPAction) workerRMI.lookup("ftpAction");
					return stubFTP;
				} catch (RemoteException | NotBoundException | InterruptedException e) {
					log.error("Error al conectar con el Servidor " + ip + ":" + port);
					return connectRandomRMIFtp();
				}
			}
		} else {
			log.error("No hay ningun servidor disponible.");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			return connectRandomRMIFtp();
		}
	}
}
