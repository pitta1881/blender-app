package blender.distributed.Worker;

import blender.distributed.Gateway.Servidor.IServidorWorkerAction;

import java.rmi.RemoteException;

public class WorkerAliveThread implements Runnable{

	private IServidorWorkerAction stubGateway;
	private String workerName;
	
	public WorkerAliveThread(IServidorWorkerAction stubServer, String workerName) {
		this.stubGateway = stubServer;
		this.workerName = workerName;
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				Thread.sleep(5000);
				this.stubGateway.pingAlive(this.workerName);
			} catch (RemoteException | InterruptedException | NullPointerException e) {
			}
		}
	}

}
