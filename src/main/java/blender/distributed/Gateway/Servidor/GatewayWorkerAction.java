package blender.distributed.Gateway.Servidor;

import blender.distributed.Enums.EServicio;
import blender.distributed.Gateway.Tools;
import blender.distributed.Records.RServidor;
import blender.distributed.Servidor.Worker.IWorkerAction;
import org.slf4j.Logger;

import java.rmi.RemoteException;
import java.util.List;

public class GatewayWorkerAction implements IWorkerAction {
	Logger log;
	List<RServidor> listaServidores;

	public GatewayWorkerAction(List<RServidor> listaServidores, Logger log) {
		this.listaServidores = listaServidores;
		this.log = log;
	}

	@Override
	public void helloServer(String workerName) {
		try {
			Tools.<IWorkerAction>connectRandomServidorRMI(this.listaServidores, EServicio.WORKER_ACTION, this.log).helloServer(workerName);
		} catch (RemoteException | NullPointerException e) {
			pingAlive(workerName);
		}
	}

	@Override
	public void pingAlive(String workerName) {
		try {
			Tools.<IWorkerAction>connectRandomServidorRMI(this.listaServidores, EServicio.WORKER_ACTION, this.log).pingAlive(workerName);
		} catch (RemoteException | NullPointerException e) {
			pingAlive(workerName);
		}
	}

	@Override
	public String getWorkToDo(String workerName) {
		try {
			return Tools.<IWorkerAction>connectRandomServidorRMI(this.listaServidores, EServicio.WORKER_ACTION, this.log).getWorkToDo(workerName);
		} catch (RemoteException | NullPointerException e) {
			return getWorkToDo(workerName);
		}
	}

	@Override
	public void setParteDone(String workerName, String parteUUID, byte[] zipParteWithRenderedImages) {
		try {
			Tools.<IWorkerAction>connectRandomServidorRMI(this.listaServidores, EServicio.WORKER_ACTION, this.log).setParteDone(workerName, parteUUID, zipParteWithRenderedImages);
		} catch (RemoteException | NullPointerException e) {
			setParteDone(workerName, parteUUID, zipParteWithRenderedImages);
		}
	}

	@Override
	public byte[] getBlendFile(String gStorageBlendName) {
		try {
			return Tools.<IWorkerAction>connectRandomServidorRMI(this.listaServidores, EServicio.WORKER_ACTION, this.log).getBlendFile(gStorageBlendName);
		} catch (RemoteException | NullPointerException e) {
			return getBlendFile(gStorageBlendName);
		}
	}


}
