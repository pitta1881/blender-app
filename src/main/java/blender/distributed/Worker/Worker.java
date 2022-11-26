package blender.distributed.Worker;

import blender.distributed.Records.RGateway;
import blender.distributed.Records.RTrabajoParte;
import blender.distributed.SharedTools.DirectoryTools;
import blender.distributed.SharedTools.RefreshListaGatewaysThread;
import blender.distributed.Worker.Threads.SendPingAliveThread;
import blender.distributed.Worker.Threads.WorkerProcessThread;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.Inet4Address;
import java.net.URL;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static blender.distributed.Worker.Tools.connectRandomGatewayRMI;


public class Worker {
	//General
	Logger log = LoggerFactory.getLogger(Worker.class);
	String blenderPortableZip;
	String workerDir = System.getProperty("user.dir") + "\\src\\main\\resources\\Worker\\";
	String workerName = "worker1663802677985"; //"worker1663802677984"; //"worker1663802677985"; //"worker"+System.currentTimeMillis();
	String singleWorkerDir = workerDir+"\\"+workerName+"\\"; ;
	String urlBlenderPortable;
	String blenderExe;
	String blenderDir;
	String worksDir;
	String blendDir;
	String rendersDir;
	String localIp;
	//server
	RedisClient redisPubClient;
	private String redisPubIp;
	private int redisPubPort;
	private String redisPubPassword;
	List<RGateway> listaGateways = new ArrayList<>();
	Gson gson = new Gson();
	Type RListaGatewayType = new TypeToken<List<RGateway>>(){}.getType();
	Type RTrabajoParteType = new TypeToken<RTrabajoParte>(){}.getType();

	public Worker () {
		MDC.put("log.name", this.getClass().getSimpleName());
		log.info("Iniciando Worker -> " + this.workerName);
		readConfigFile();
		runRedisPubClient();
		createThreadRefreshListaGateways();
		createThreadSendPingAlive();
		if (checkNeededFiles()) {
			getWork();
		} else {
			log.debug("Error inesperado!");
		}
	}

	private void createThreadSendPingAlive() {
		SendPingAliveThread aliveT = new SendPingAliveThread(this.listaGateways, this.workerName);
		Thread threadAliveT = new Thread(aliveT);
		threadAliveT.start();
	}
	private void createThreadRefreshListaGateways() {
		RefreshListaGatewaysThread listaGatewaysT = new RefreshListaGatewaysThread(this.listaGateways, this.redisPubClient);
		Thread threadAliveT = new Thread(listaGatewaysT);
		threadAliveT.start();
	}

	private void getWork() {
		DirectoryTools.checkOrCreateFolder(this.worksDir);
		while (true) {
			String recordTrabajoParteJson = null;
			RTrabajoParte recordTrabajoParte = null;
				while (recordTrabajoParte == null){
					try {
						recordTrabajoParteJson = connectRandomGatewayRMI(this.listaGateways).getWorkToDo(this.workerName);
						if(recordTrabajoParteJson == null) {
							Thread.sleep(1000);
						}
					} catch (InterruptedException | RemoteException e) {
					}
				}
				recordTrabajoParte = gson.fromJson(recordTrabajoParteJson, RTrabajoParteType);
				File thisWorkDir = new File(this.worksDir + recordTrabajoParte.rParte().uuid());
				DirectoryTools.checkOrCreateFolder(thisWorkDir.getAbsolutePath());
				File thisWorkRenderDir = new File(thisWorkDir + this.rendersDir);
				DirectoryTools.checkOrCreateFolder(thisWorkRenderDir.getAbsolutePath());
				File thisWorkBlendDir = new File(thisWorkDir + this.blendDir);
				DirectoryTools.checkOrCreateFolder(thisWorkBlendDir.getAbsolutePath());
				log.info("Recibi un nuevo trabajo: " + recordTrabajoParte.rParte().uuid());
				byte[] blendFileBytes = downloadBlendFileByURL(recordTrabajoParte.rTrabajo().urlBlendFile());
				File blendFile = persistBlendFile(blendFileBytes, thisWorkBlendDir.getAbsolutePath(), recordTrabajoParte.rTrabajo().blendName());
				startRender(recordTrabajoParte, thisWorkRenderDir.getAbsolutePath(), blendFile);
				DirectoryTools.deleteDirectory(thisWorkDir);
		}
	}

	private byte[] downloadBlendFileByURL(String urlBlendFile) {
		return new byte[0];
	}

	private void downloadBlendAppPortable() {
		log.info("Intentando descargar... Porfavor espere, este proceso podria tardar varios minutos...");
		try {
			FileUtils.copyURLToFile(
					//new URL(this.urlBlenderPortable),
					new URL("file:\\E:\\Bibliotecas\\Desktop\\blender-windows.zip"),
					new File(this.singleWorkerDir + this.blenderPortableZip),
					10000,
					10000);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	private File persistBlendFile(byte[] byteBlend, String thisWorkBlendDir, String blendName) {
		File blend = new File(thisWorkBlendDir + "\\" + blendName);
		try (FileOutputStream fos = new FileOutputStream(blend)) {
			fos.write(byteBlend);
		} catch (Exception e) {
			log.error("ERROR: " + e.getMessage());
		}
		return blend;
	}

	private void startRender(RTrabajoParte recordTrabajoParte, String thisWorkRenderDir, File blendFile) {
		//Formato: blender -b file_name.blend -f 1 //blender -b file_name.blend -s 1 -e 100 -a
		log.info("Pre-configurando el archivo .blend");
		String cmd;
		int totalFrames = recordTrabajoParte.rParte().endFrame() - recordTrabajoParte.rParte().startFrame();
		int threadsNedeed = 1;
		CountDownLatch latch;
		List<WorkerProcessThread> workerThreads = new ArrayList<>();
		if(totalFrames == 0) {
			cmd = " -b \"" + blendFile.getAbsolutePath() + "\" -o \"" + thisWorkRenderDir + "\\frame_#####\"" + " -f " + recordTrabajoParte.rParte().startFrame();
			latch = new CountDownLatch(threadsNedeed);
			File f = new File(this.blenderExe + cmd);//Normalize backslashs and slashs
			System.out.println("CMD: " + f.getAbsolutePath());
			workerThreads.add(new WorkerProcessThread(latch, f.getAbsolutePath()));
		} else {
			if(totalFrames >= 300){
				threadsNedeed = 8;
			} else if (totalFrames >= 100) {
				threadsNedeed = 4;
			} else if (totalFrames >= 50) {
				threadsNedeed = 2;
			}
			log.info("Cantidad de Frames a renderizar: " + (totalFrames + 1));
			log.info("Cantidad de Threads a crear: " + threadsNedeed);
			int rangeFrame = (int) Math.ceil((float)totalFrames / (float)threadsNedeed);
			int startFrame = recordTrabajoParte.rParte().startFrame();
			int endFrame = startFrame + rangeFrame;
			latch = new CountDownLatch(threadsNedeed);
			for (int i = 0; i < threadsNedeed; i++) {
				cmd = " -b \"" + blendFile.getAbsolutePath() + "\" -o \"" + thisWorkRenderDir + "\\frame_#####\"" + " -s " + startFrame + " -e " + endFrame + " -a";
				File f = new File(this.blenderExe + cmd);//Normalize backslashs and slashs
				log.info("CMD: " + f.getAbsolutePath());
				workerThreads.add(new WorkerProcessThread(latch, f.getAbsolutePath()));
				startFrame = endFrame + 1;
				endFrame += rangeFrame;
				if(endFrame > recordTrabajoParte.rParte().endFrame()){
					endFrame = recordTrabajoParte.rParte().endFrame();
				}
			}
		}
		Executor executor = Executors.newFixedThreadPool(workerThreads.size());
		for(final WorkerProcessThread wt : workerThreads) {
			executor.execute(wt);
		}
		try {
			latch.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		try {
			new ZipFile(thisWorkRenderDir + recordTrabajoParte.rTrabajo().blendName()+".zip").addFolder(new File(thisWorkRenderDir));
		} catch (ZipException e) {
			throw new RuntimeException(e);
		}
		try {
			File zipRenderedImages = new File(thisWorkRenderDir + recordTrabajoParte.rTrabajo().blendName()+".zip");
			byte[] zipWithRenderedImages = Files.readAllBytes(zipRenderedImages.toPath());
			boolean zipSent = false;
			while(!zipSent) {
				try {
					connectRandomGatewayRMI(this.listaGateways).setParteDone(this.workerName, recordTrabajoParte.rParte().uuid(), zipWithRenderedImages);
					zipSent = true;
				} catch (IOException e) {
					log.error("Conexión perdida con el Servidor.");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("==========Termine=========");
	}



	private void runRedisPubClient() {
		this.redisPubClient = RedisClient.create("redis://"+this.redisPubPassword+"@"+this.redisPubIp+":"+this.redisPubPort);
		log.info("Conectado a Redis Público exitosamente.");
		StatefulRedisConnection redisConnection = this.redisPubClient.connect();
		RedisCommands commands = redisConnection.sync();
		this.listaGateways = gson.fromJson(String.valueOf(commands.hvals("listaGateways")), RListaGatewayType);
		redisConnection.close();
	}

	/*
	 * Verifica si el worker tiene las carpetas necesarias para trabajar,
	 * en caso de no tenerlas las descarga por ftp.
	 */
	private boolean checkNeededFiles() {
		File singleWorkerDir = new File(this.singleWorkerDir);
		DirectoryTools.checkOrCreateFolder(singleWorkerDir.getAbsolutePath());
		long size = DirectoryTools.getFolderSize(singleWorkerDir);
		log.info("Obteniendo tamanio de: " + singleWorkerDir.getAbsolutePath() + " MB:" + (size / 1024));
		if (size < 30000000) {
			downloadBlenderApp();
		} else {
			log.info("Blender ----> LISTO");
		}
		return true;
	}

	private void downloadBlenderApp() {
		log.info("La carpeta BlenderApp esta corrupta o no existe. Descargandola...");
		downloadBlendAppPortable();
		log.info("Comenzando a UnZipear Blender... Porfavor espere, este proceso podria tardar varios minutos...");
		try {
			new ZipFile(this.singleWorkerDir + this.blenderPortableZip).extractAll(this.singleWorkerDir);
		} catch (ZipException e) {
			throw new RuntimeException(e);
		}
		log.info("Unzip Blender terminado.");
		File zipFileBlender = new File(this.singleWorkerDir + this.blenderPortableZip);
		zipFileBlender.delete();
		File directoryPath = new File(this.singleWorkerDir);
		String contents[] = directoryPath.list();
		File dirToRename = new File(this.singleWorkerDir + contents[0]);
		File newDir = new File(this.blenderDir);
		dirToRename.renameTo(newDir);
	}

	private void readConfigFile() {
		Gson gson = new Gson();
		Map config;
		try {
			this.localIp = Inet4Address.getLocalHost().getHostAddress();
			config = gson.fromJson(new FileReader(this.workerDir + "config.json"), Map.class);

			Map redisPub = (Map) config.get("redis_pub");
			this.redisPubIp = redisPub.get("ip").toString();
			this.redisPubPort = Integer.valueOf(redisPub.get("port").toString());
			this.redisPubPassword = redisPub.get("password").toString();

			Map paths = (Map) config.get("paths");
			this.urlBlenderPortable = paths.get("urlBlenderPortable").toString();
			this.blenderExe = this.singleWorkerDir + paths.get("blenderExe");
			this.blenderDir = this.singleWorkerDir + paths.get("blenderDir");
			this.worksDir = this.singleWorkerDir + paths.get("worksDir");
			this.blendDir = paths.get("blendDir").toString();
			this.rendersDir = paths.get("rendersDir").toString();
			this.blenderPortableZip = paths.get("blenderPortableZip").toString();

		} catch (IOException e) {
			log.error("Error Archivo Config!");
		}
	}

	public static void main(String[] args) { new Worker(); }

}
