package blender.distributed.Servidor.Cliente;

import blender.distributed.Servidor.SerializedObjectCodec;
import blender.distributed.Servidor.ThreadServer;
import blender.distributed.Servidor.Trabajo.Trabajo;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ClienteAction implements IClientAction {
	Logger log = LoggerFactory.getLogger(ClienteAction.class);
	String redisIp;
	int redisPort;
	RedisClient redisClient;
	byte[] listaTrabajosByte = SerializationUtils.serialize("listaTrabajos");


	public ClienteAction(RedisClient redisClient) {
		MDC.put("log.name", ClienteAction.class.getSimpleName());
		this.redisClient = redisClient;
	}

	@Override
	public byte[] renderRequest(Trabajo work) {
		StatefulRedisConnection<String, Object> redisConnection = this.redisClient.connect(new SerializedObjectCodec());
		RedisCommands<String, Object> syncCommands = redisConnection.sync();
		syncCommands.hset("listaTrabajos", work.getId(), work);

		CountDownLatch latch = new CountDownLatch(1);
		List<ThreadServer> serverThread = new ArrayList<>();
		serverThread.add(new ThreadServer(latch, this.redisClient, work));
		Executor executor = Executors.newFixedThreadPool(serverThread.size());
		for(final ThreadServer wt : serverThread) {
			executor.execute(wt);
		}
		try {
			latch.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		syncCommands.hdel("listaTrabajos", work.getId());
		redisConnection.close();
		return work.getZipWithRenderedImages();
	}
}
