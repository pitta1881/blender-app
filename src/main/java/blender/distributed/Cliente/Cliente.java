package blender.distributed.Cliente;

import blender.distributed.Cliente.Threads.CheckFinishedTrabajo;
import blender.distributed.Records.RGateway;
import blender.distributed.Records.RTrabajo;
import blender.distributed.SharedTools.RefreshListaGatewaysThread;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.cdimascio.dotenv.Dotenv;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.util.List;

import static blender.distributed.Cliente.Tools.connectRandomGatewayRMI;

public class Cliente{
	Logger log = LoggerFactory.getLogger(this.getClass());
	File file;
	byte[] fileContent;
	List<RGateway> listaGateways;
	RedisClient redisPubClient;
	private String redisPubURI;
	Type RTrabajoType = new TypeToken<RTrabajo>(){}.getType();
	Dotenv dotenv = Dotenv.load();

	public Cliente() {
		readConfigFile();
		runRedisPubClient();
		createThreadRefreshListaGateways();
		MDC.put("log.name", Cliente.class.getSimpleName());
	}

	private void createThreadRefreshListaGateways() {
		RefreshListaGatewaysThread listaGatewaysT = new RefreshListaGatewaysThread(this.listaGateways, this.redisPubClient);
		Thread threadAliveT = new Thread(listaGatewaysT);
		threadAliveT.start();
	}

	private void runRedisPubClient() {
		this.redisPubClient = RedisClient.create(this.redisPubURI);
		StatefulRedisConnection redisConnection = this.redisPubClient.connect();
		log.info("Conectado a Redis Público exitosamente.");
		RedisCommands commands = redisConnection.sync();
		this.listaGateways = new Gson().fromJson(String.valueOf(commands.hvals("listaGateways")), new TypeToken<List<RGateway>>(){}.getType());
		redisConnection.close();
	}

	public void setFile(File f) {
		try {
			this.file = f;
			this.fileContent = Files.readAllBytes(file.toPath());
		} catch (IOException e) {
			log.error("Error: " + e.getMessage());
		}
	}
	public void enviarFile(int startFrame, int endFrame) {
		if(this.file != null) {
			log.info("Enviando el archivo: "+this.file.getName());
			try {
				String recordTrabajoJson = connectRandomGatewayRMI(this.listaGateways).renderRequest(this.fileContent, file.getName(), startFrame, endFrame);
				RTrabajo recordTrabajo = new Gson().fromJson(recordTrabajoJson, RTrabajoType);
				createThreadCheckFinishedTrabajo(recordTrabajo);
			} catch (RemoteException e) {
				log.error("Error: " + e.getMessage());
			}
		}else {
			log.error("Error: Archivo no cargado");
		}
	}
	public boolean isReady() {
		return (this.file != null && this.fileContent != null);
	}

	private void readConfigFile() {
		this.redisPubURI = "redis://"+dotenv.get("REDIS_PUBLIC_USER")+":"+dotenv.get("REDIS_PUBLIC_PASS")+"@"+dotenv.get("REDIS_PUBLIC_IP")+":"+dotenv.get("REDIS_PUBLIC_PORT");
	}

	private void createThreadCheckFinishedTrabajo(RTrabajo recordTrabajo) {
		CheckFinishedTrabajo checkFinishedTrabajoT = new CheckFinishedTrabajo(this.listaGateways, recordTrabajo);
		Thread threadListaT = new Thread(checkFinishedTrabajoT);
		threadListaT.start();
	}

}

