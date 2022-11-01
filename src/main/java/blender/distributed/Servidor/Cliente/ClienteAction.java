package blender.distributed.Servidor.Cliente;

import blender.distributed.Servidor.ThreadServer;
import blender.distributed.Servidor.Trabajo.Trabajo;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ClienteAction implements IClientAction {
	Logger log = LoggerFactory.getLogger(ClienteAction.class);
	JedisPool pool;
	byte[] listaTrabajosByte = SerializationUtils.serialize("listaTrabajos");


	public ClienteAction(JedisPool pool) {
		this.pool = pool;
		MDC.put("log.name", ClienteAction.class.getSimpleName());
	}

	@Override
	public byte[] renderRequest(Trabajo work) {
		byte[] idByte = SerializationUtils.serialize(work.getId());
		try (Jedis jedis = this.pool.getResource()) {
			jedis.hset(this.listaTrabajosByte, idByte, SerializationUtils.serialize(work));
		} catch (Exception e) {
			e.printStackTrace();
		}
		CountDownLatch latch = new CountDownLatch(1);
		List<ThreadServer> serverThread = new ArrayList<>();
		serverThread.add(new ThreadServer(latch, this.pool, work));
		Executor executor = Executors.newFixedThreadPool(serverThread.size());
		for(final ThreadServer wt : serverThread) {
			executor.execute(wt);
		}
		try {
			latch.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		try (Jedis jedis = this.pool.getResource()) {
			jedis.hdel(this.listaTrabajosByte, idByte);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return work.getZipWithRenderedImages();
	}
}
