package blender.distributed.Worker.Threads;

import blender.distributed.Enums.ENodo;
import blender.distributed.Records.RGateway;
import blender.distributed.Worker.Worker;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.rmi.RemoteException;
import java.util.List;

import static blender.distributed.Worker.Tools.connectRandomGatewayRMI;

public class SendPingAliveThread implements Runnable{
	Logger log;
	private List<RGateway> listaGateways;
	private String workerName;
	
	public SendPingAliveThread(List<RGateway> listaGateways, String workerName, Logger log) {
		MDC.put("log.name", ENodo.WORKER.name());
		this.listaGateways = listaGateways;
		this.workerName = workerName;
		this.log = log;
	}
	
	@Override
	public void run() {
		MDC.put("log.name", ENodo.WORKER.name());
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
