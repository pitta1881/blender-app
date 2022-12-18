package blender.distributed.Worker.Threads;

import blender.distributed.Enums.EServicio;
import blender.distributed.Records.RGateway;
import blender.distributed.Servidor.Worker.IWorkerAction;
import blender.distributed.SharedTools.Tools;
import org.slf4j.Logger;

import java.rmi.RemoteException;
import java.util.List;

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
				Tools.<IWorkerAction>connectRandomGatewayRMI(this.listaGateways, EServicio.WORKER_ACTION, -1, this.log).pingAlive(this.workerName);
			} catch (RemoteException | InterruptedException e) {
				log.error("Error: " + e.getMessage());
			}
		}
	}

}
