package blender.distributed.Servidor;

import blender.distributed.Servidor.Trabajo.Trabajo;
import blender.distributed.Servidor.Trabajo.TrabajoStatus;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.CountDownLatch;

public class ThreadServer implements Runnable {
	Logger log = LoggerFactory.getLogger(ThreadServer.class);
	Trabajo work;
	private final CountDownLatch latchSignal;
	JedisPool pool;
	byte[] listaTrabajosByte = SerializationUtils.serialize("listaTrabajos");


	public ThreadServer(CountDownLatch latch, JedisPool pool, Trabajo work) {
		this.latchSignal = latch;
		this.work = work;
		this.pool = pool;
	}

	@Override
	public void run() {
		boolean salir = false;
		LocalTime initTime = LocalTime.now();
		log.info("Trabajo iniciado: " + work.getId());
		log.info("Trabajando en: "+work.getBlendName()+" - Frames " + work.getStartFrame() + "-" + work.getEndFrame());
		log.info("Tiempo inicio:\t"+initTime.toString());
		byte[] idByte = SerializationUtils.serialize(work.getId());
		while(!salir) {
			try {
				Trabajo trabajo = null;
				try (Jedis jedis = this.pool.getResource()) {
					byte[] workByte = jedis.hget(this.listaTrabajosByte, idByte);
					trabajo = SerializationUtils.deserialize(workByte);
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (trabajo.getStatus() == TrabajoStatus.DONE && trabajo.getZipWithRenderedImages() != null) {
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
		this.latchSignal.countDown();
	}
}
