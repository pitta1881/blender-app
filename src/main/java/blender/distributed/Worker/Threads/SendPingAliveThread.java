package blender.distributed.Worker.Threads;

import blender.distributed.Records.RGateway;

import java.rmi.RemoteException;
import java.util.List;

import static blender.distributed.Worker.Tools.connectRandomGatewayRMI;

public class SendPingAliveThread implements Runnable{
	private List<RGateway> listaGateways;
	private String workerName;
	
	public SendPingAliveThread(List<RGateway> listaGateways, String workerName) {
		this.listaGateways = listaGateways;
		this.workerName = workerName;
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				Thread.sleep(5000);
				connectRandomGatewayRMI(this.listaGateways).pingAlive(this.workerName);
			} catch (RemoteException | InterruptedException e) {
			}
		}
	}

}
