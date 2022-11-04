package blender.distributed.Servidor;

import blender.distributed.Servidor.Trabajo.Trabajo;
import blender.distributed.Servidor.Trabajo.TrabajoStatus;
import io.lettuce.core.RedisClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.CountDownLatch;

public class ThreadServer implements Runnable {
	Logger log = LoggerFactory.getLogger(ThreadServer.class);
	Trabajo work;
	private final CountDownLatch latchSignal;
	RedisClient redisClient;


	public ThreadServer(CountDownLatch latch, RedisClient redisClient, Trabajo work) {
		this.latchSignal = latch;
		this.redisClient = redisClient;
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
				Trabajo trabajo = null;
				/*
				try (Jedis jedis = this.pool.getResource()) {
					byte[] workByte = jedis.hget(this.listaTrabajosByte, idByte);
					trabajo = SerializationUtils.deserialize(workByte);
				} catch (Exception e) {
					e.printStackTrace();
				}

				 */
				if (trabajo != null && trabajo.getStatus() == TrabajoStatus.DONE && trabajo.getZipWithRenderedImages() != null) {
					salir = true;
				}
				Thread.sleep(500);
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
