package blender.distributed.Cliente;

import blender.distributed.Cliente.Threads.CheckFinishedTrabajo;
import blender.distributed.Gateway.Gateway;
import blender.distributed.Records.RGateway;
import blender.distributed.Records.RTrabajo;
import blender.distributed.SharedTools.DirectoryTools;
import blender.distributed.SharedTools.RefreshListaGatewaysThread;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.cdimascio.dotenv.Dotenv;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import static blender.distributed.Cliente.Tools.connectRandomGatewayRMI;

public class Cliente{
	Logger log = LoggerFactory.getLogger(this.getClass());
	File file;
	byte[] fileContent;
	String appDir = System.getProperty("user.dir") + "/app/";
	String clienteDirectory = appDir + "Cliente/";
	String myRenderedImages;
	List<RGateway> listaGateways;
	RedisClient redisPubClient;
	private String redisPubURI;
	Gson gson = new Gson();
	Type RTrabajoType = new TypeToken<RTrabajo>(){}.getType();
	Dotenv dotenv = Dotenv.load();

	public Cliente() {
		readConfigFile();
		runRedisPubClient();
		createThreadRefreshListaGateways();
		createDirectories();
		MDC.put("log.name", Cliente.class.getSimpleName());
	}

	private void createDirectories(){
		File appDir = new File(this.appDir);
		File clienteDir = new File(this.clienteDirectory);
		DirectoryTools.checkOrCreateFolder(appDir.getAbsolutePath());
		DirectoryTools.checkOrCreateFolder(clienteDir.getAbsolutePath());
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
			e.printStackTrace();
		}
	}
	public void enviarFile(int startFrame, int endFrame) {
		if(this.file != null) {
			log.info("Enviando el archivo: "+this.file.getName());
			try {
				String recordTrabajoJson = connectRandomGatewayRMI(this.listaGateways).renderRequest(this.fileContent, file.getName(), startFrame, endFrame);
				RTrabajo recordTrabajo = gson.fromJson(recordTrabajoJson, RTrabajoType);
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
		Gson gson = new Gson();
		Map config;
		try {
			InputStream stream = this.getClass().getClassLoader().getResourceAsStream("Cliente/config.json");
			config = gson.fromJson(IOUtils.toString(stream, "UTF-8"), Map.class);

			this.redisPubURI = "redis://"+dotenv.get("REDIS_PUBLIC_USER")+":"+dotenv.get("REDIS_PUBLIC_PASS")+"@"+dotenv.get("REDIS_PUBLIC_IP")+":"+dotenv.get("REDIS_PUBLIC_PORT");

			Map paths = (Map) config.get("paths");
			this.myRenderedImages = this.clienteDirectory + paths.get("renderedImages");

		} catch (IOException e) {
			log.error("Error Archivo Config!");
		} 
	}

	private void createThreadCheckFinishedTrabajo(RTrabajo recordTrabajo) {
		CheckFinishedTrabajo checkFinishedTrabajoT = new CheckFinishedTrabajo(this.listaGateways, recordTrabajo);
		Thread threadListaT = new Thread(checkFinishedTrabajoT);
		threadListaT.start();
	}

}

