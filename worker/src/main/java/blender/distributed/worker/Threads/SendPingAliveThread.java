package blender.distributed.worker.Threads;

import blender.distributed.shared.Records.RGateway;
import org.slf4j.Logger;


import java.rmi.RemoteException;
import java.util.List;

import static blender.distributed.worker.Tools.connectRandomGatewayRMI;


public class SendPingAliveThread implements Runnable{
	Logger log;
	private List<RGateway> listaGateways;
	private String workerName;
	
	public SendPingAliveThread(List<RGateway> listaGateways, String workerName, Logger log) {
		this.listaGateways = listaGateways;
		this.workerName = workerName;
		this.log = log;
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				Thread.sleep(5000);
				connectRandomGatewayRMI(this.listaGateways, this.log).pingAlive(this.workerName);
			} catch (RemoteException | InterruptedException e) {
				log.error("Error: " + e.getMessage());
			}
		}
	}

}
