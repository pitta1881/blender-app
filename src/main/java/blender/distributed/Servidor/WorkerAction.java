package blender.distributed.Servidor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.rmi.RemoteException;
import java.time.LocalTime;
import java.util.*;

public class WorkerAction implements IWorkerAction{
	ArrayList<String> listaWorkers;
	ArrayList<String> listaTrabajos;
	ArrayList<String> workerToRemove;
	Map<String,LocalTime> workersLastPing = new HashMap<String,LocalTime>();
	Logger log = LoggerFactory.getLogger(WorkerAction.class);

	public WorkerAction(ArrayList<String> listaWorkers, ArrayList<String> listaTrabajos, Map<String, LocalTime> workersLastPing) {
		MDC.put("log.name", WorkerAction.class.getSimpleName().toString());
		this.listaWorkers = listaWorkers;
		this.listaTrabajos = listaTrabajos;
		this.workersLastPing = workersLastPing;
	}

	@Override
	public void helloServer(String worker) throws RemoteException {
		synchronized (listaTrabajos) {
			if(!listaWorkers.contains(worker)) {
				this.listaWorkers.add(worker);
				log.info("Registrando nuevo worker: "+worker);
			}
		}
		synchronized (workersLastPing) {
			workersLastPing.put(worker,LocalTime.now());
		}
	}

	@Override
	public void checkStatus() throws RemoteException {
		
	}
}
