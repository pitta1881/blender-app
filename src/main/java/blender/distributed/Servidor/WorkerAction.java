package blender.distributed.Servidor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.rmi.RemoteException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WorkerAction implements IWorkerAction{
	ArrayList<String> listaWorkers;
	ArrayList<Mensaje> listaTrabajos;
	ArrayList<String> workerToRemove;
	Map<String,LocalTime> workersLastPing = new HashMap<String,LocalTime>();
	Logger log = LoggerFactory.getLogger(WorkerAction.class);

	public WorkerAction(ArrayList<String> listaWorkers, ArrayList<Mensaje> listaTrabajos, Map<String, LocalTime> workersLastPing) {
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

	@Override
	public Mensaje giveWorkToDo(String worker) throws RemoteException {
		if(listaTrabajos.size() == 0){
			return new Mensaje("empty");
		}
		int i = 0;
		Mensaje trabajo = listaTrabajos.get(i);
		while(trabajo.getStatus() != 1 &&  i < listaTrabajos.size()){
			trabajo = listaTrabajos.get(i);
			i++;
		}
		if(trabajo.getStatus() == 1){
			trabajo.setStatus(2);
			return trabajo;
		} else {
			return new Mensaje("empty");
		}
	}

	@Override
	public void setTrabajoStatusDone(Mensaje msj) throws RemoteException {
		for (Mensaje trabajo : listaTrabajos) {
			if(trabajo.getName().equals(msj.getName())){
				trabajo.setStatus(3);
				trabajo.setZipWithRenderedImages(msj.getZipWithRenderedImages());
			}
		}
	}
}
