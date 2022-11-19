package blender.distributed.Worker;

import blender.distributed.Gateway.Servidor.IGatewayWorkerAction;

import java.rmi.RemoteException;

public class WorkerAliveThread implements Runnable{

	private IGatewayWorkerAction stubGateway;
	private String workerName;
	
	public WorkerAliveThread(IGatewayWorkerAction stubServer, String workerName) {
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
