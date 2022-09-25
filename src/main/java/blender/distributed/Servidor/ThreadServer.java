package blender.distributed.Servidor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;

public class ThreadServer implements Runnable {
	private final int TIME_OUT = 100;
	Logger log = LoggerFactory.getLogger(ThreadServer.class);
	Mensaje msg;
	ArrayList<Mensaje> listaTrabajos;
	byte[] zipWithRenderedImages = null;

	public ThreadServer(Mensaje msg, ArrayList<Mensaje> listaTrabajos) {
		this.msg = msg;
		this.listaTrabajos = listaTrabajos;
	}
	
	public byte[] getZipWithRenderedImage() {
		return this.zipWithRenderedImages;
	}
	
	@Override
	public void run() {
		boolean salir = false;
		LocalTime initTime = LocalTime.now();
		log.info("Trabajando en: "+msg.getName()+" - Frame Nº "+msg.getStartFrame());
		log.info("Tiempo inicio:\t"+initTime.toString());
		while(!salir) {
			try {
				if (msg.getStatus() == 3) {
					this.zipWithRenderedImages = msg.getZipWithRenderedImages();
					salir = true;
				} else {
					Thread.sleep(1000);
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		LocalTime finishTime = LocalTime.now();
		log.info("Tiempo fin:\t"+finishTime.toString());
		log.info("Tiempo tardado:\t\t"+Duration.between(initTime, finishTime).toSeconds()+" segundos.");
	}
}
