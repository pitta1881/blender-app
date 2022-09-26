package blender.distributed.Servidor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalTime;

public class ThreadServer implements Runnable {
	Logger log = LoggerFactory.getLogger(ThreadServer.class);
	Trabajo work;

	public ThreadServer(Trabajo work) {
		this.work = work;
	}

	@Override
	public void run() {
		boolean salir = false;
		LocalTime initTime = LocalTime.now();
		log.info("Trabajo iniciado: " + work.getId());
		log.info("Trabajando en: "+work.getBlendName()+" - Frames " + work.getStartFrame() + "-" + work.getEndFrame());
		log.info("Tiempo inicio:\t"+initTime.toString());
		while(!salir) {
			try {
				if (work.getStatus() == 3) {
					salir = true;
				} else {
					Thread.sleep(500);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		LocalTime finishTime = LocalTime.now();
		log.info("Tiempo fin:\t"+finishTime.toString());
		log.info("Tiempo tardado:\t\t"+Duration.between(initTime, finishTime).toSeconds()+" segundos.");
		log.info("Trabajo completado: " + work.getId());
	}
}
