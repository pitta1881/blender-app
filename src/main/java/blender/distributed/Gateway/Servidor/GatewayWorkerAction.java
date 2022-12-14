package blender.distributed.Gateway.Servidor;

import blender.distributed.Enums.ENodo;
import blender.distributed.Records.RServidor;
import blender.distributed.Servidor.Worker.IWorkerAction;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Random;

import static blender.distributed.SharedTools.Tools.manageGatewayServidorFall;

public class GatewayWorkerAction implements IWorkerAction {
	Logger log;
	List<RServidor> listaServidores;

	public GatewayWorkerAction(List<RServidor> listaServidores, Logger log) {
		MDC.put("log.name", ENodo.GATEWAY.name());
		this.listaServidores = listaServidores;
		this.log = log;
	}

	@Override
	public void helloServer(String workerName) {
		try {
			connectRandomServidorRMIForWorker().helloServer(workerName);
		} catch (RemoteException | NullPointerException e) {
			pingAlive(workerName);
		}
	}

	@Override
	public void pingAlive(String workerName) {
		try {
			connectRandomServidorRMIForWorker().pingAlive(workerName);
		} catch (RemoteException | NullPointerException e) {
			pingAlive(workerName);
		}
	}

	@Override
	public String getWorkToDo(String workerName) {
		try {
			return connectRandomServidorRMIForWorker().getWorkToDo(workerName);
		} catch (RemoteException | NullPointerException e) {
			return getWorkToDo(workerName);
		}
	}

	@Override
	public void setParteDone(String workerName, String parteUUID, byte[] zipParteWithRenderedImages) {
		try {
			connectRandomServidorRMIForWorker().setParteDone(workerName, parteUUID, zipParteWithRenderedImages);
		} catch (RemoteException | NullPointerException e) {
			setParteDone(workerName, parteUUID, zipParteWithRenderedImages);
		}
	}

	@Override
	public byte[] getBlendFile(String gStorageBlendName) {
		try {
			return connectRandomServidorRMIForWorker().getBlendFile(gStorageBlendName);
		} catch (RemoteException | NullPointerException e) {
			return getBlendFile(gStorageBlendName);
		}
	}

	private IWorkerAction connectRandomServidorRMIForWorker() {
		IWorkerAction stubServidor = null;
		if(this.listaServidores.size() > 0) {
			Random rand = new Random();
			int nRandomServidor = rand.nextInt(this.listaServidores.size());
			String ip = this.listaServidores.get(nRandomServidor).ip();
			int port = this.listaServidores.get(nRandomServidor).rmiPortForWorkers();
			try {
				Registry workerRMI = LocateRegistry.getRegistry(ip, port);
				stubServidor = (IWorkerAction) workerRMI.lookup("workerAction");
				return stubServidor;
			} catch (RemoteException | NotBoundException e) {
				log.error("Error: " + e.getMessage());
				manageGatewayServidorFall(ENodo.SERVIDOR, ip, port, this.log, ENodo.GATEWAY.name());
				return connectRandomServidorRMIForWorker();
			}
		} else {
			log.error("No hay ningun servidor disponible.");
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
			}
			return connectRandomServidorRMIForWorker();
		}
	}

}
