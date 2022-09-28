package blender.distributed.Worker;

import blender.distributed.Servidor.IWorkerAction;

import java.rmi.RemoteException;

public class WorkerAliveThread implements Runnable{

	private IWorkerAction stubServer;
	private String workerName;
	
	public WorkerAliveThread(IWorkerAction stubServer, String workerName) {
		this.stubServer = stubServer;
		this.workerName = workerName;
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				this.stubServer.helloServer(this.workerName);
				Thread.sleep(60000);
			} catch (RemoteException | InterruptedException e) {
				break;
			}
		}
	}

}
