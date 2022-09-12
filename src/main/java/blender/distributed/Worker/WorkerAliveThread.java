package blender.distributed.Worker;

import blender.distributed.Servidor.IWorkerAction;

import java.rmi.RemoteException;

public class WorkerAliveThread implements Runnable{

	private IWorkerAction stubServer;
	private String localIp;
	
	public WorkerAliveThread(IWorkerAction stubServer, String localIp) {
		this.stubServer = stubServer;
		this.localIp = localIp;
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				this.stubServer.helloServer(this.localIp);
				Thread.sleep(60000);
			} catch (RemoteException | InterruptedException e) {
				break;
			}
		}
	}

}
