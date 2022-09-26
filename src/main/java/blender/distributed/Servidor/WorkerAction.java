package blender.distributed.Servidor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.rmi.RemoteException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Map;

public class WorkerAction implements IWorkerAction{
	Logger log = LoggerFactory.getLogger(WorkerAction.class);
	Map<String,LocalTime> workersLastPing;
	ArrayList<String> listaWorkers;
	ArrayList<Trabajo> listaTrabajos;

	public WorkerAction(ArrayList<String> listaWorkers, ArrayList<Trabajo> listaTrabajos, Map<String, LocalTime> workersLastPing) {
		MDC.put("log.name", WorkerAction.class.getSimpleName());
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
	public Trabajo giveWorkToDo(String worker) throws RemoteException {
		if(listaTrabajos.size() == 0){
			return null;
		}
		int i = 0;
		Trabajo trabajo = listaTrabajos.get(i);
		while(trabajo.getStatus() != 1 &&  i < listaTrabajos.size()){
			trabajo = listaTrabajos.get(i);
			i++;
		}
		if(trabajo.getStatus() == 1){
			trabajo.setStatus(2);
			return trabajo;
		} else {
			return null;
		}
	}

	@Override
	public void setTrabajoStatusDone(String id, byte[] zipWithRenderedImages) throws RemoteException {
		Trabajo work = listaTrabajos.stream().filter(trabajo -> id.equals(trabajo.getId())).findFirst().orElse(null);
		if(work != null) {
			work.setStatus(3);
			work.setZipWithRenderedImages(zipWithRenderedImages);
		}
	}
}
