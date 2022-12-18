package blender.distributed.Worker;

import blender.distributed.Enums.EServicio;
import blender.distributed.Records.RGateway;
import blender.distributed.Records.RTrabajoParte;
import blender.distributed.Servidor.Worker.IWorkerAction;
import blender.distributed.SharedTools.DirectoryTools;
import blender.distributed.SharedTools.RefreshListaGatewaysThread;
import blender.distributed.SharedTools.Tools;
import blender.distributed.Worker.Threads.SendPingAliveThread;
import blender.distributed.Worker.Threads.WorkerProcessThread;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.cdimascio.dotenv.Dotenv;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

public class Worker {
	//General
	private static final Logger log = LoggerFactory.getLogger(Worker.class);
	String blenderPortable;
	String appDir = System.getProperty("user.dir") + "/app/";
	String workerDir = appDir + "Worker/";
	String workerName = "worker"+System.currentTimeMillis();
	String singleWorkerDir = workerDir + "/" + workerName + "/";
	String urlBlenderPortable;
	String blenderExe;
	String blenderDir;
	String worksDir;
	String blendDir;
	String rendersDir;
	String localIp;
	//server
	RedisClient redisPubClient;
	private String redisPubURI;
	List<RGateway> listaGateways = new ArrayList<>();
	Gson gson = new Gson();
	Type RListaGatewayType = new TypeToken<List<RGateway>>(){}.getType();
	Type RTrabajoParteType = new TypeToken<RTrabajoParte>(){}.getType();
	Dotenv dotenv = Dotenv.load();

	public Worker () {
		readConfigFile();
		runRedisPubClient();
		if (checkNeededFiles()) {
			createThreadRefreshListaGateways();
			helloServer();
			createThreadSendPingAlive();
			getWork();
		} else {
			log.debug("Error inesperado!");
		}
	}

	private void helloServer() {
		try {
			Thread.sleep(1000);
			Tools.<IWorkerAction>connectRandomGatewayRMI(this.listaGateways, EServicio.WORKER_ACTION, -1, this.log).helloServer(this.workerName);
		} catch (RemoteException | InterruptedException e) {
			log.error("Error: " + e.getMessage());
			helloServer();
		}
	}

	private void createThreadSendPingAlive() {
		SendPingAliveThread aliveT = new SendPingAliveThread(this.listaGateways, this.workerName, this.log);
		Thread threadAliveT = new Thread(aliveT);
		threadAliveT.start();
	}
	private void createThreadRefreshListaGateways() {
		RefreshListaGatewaysThread listaGatewaysT = new RefreshListaGatewaysThread(this.listaGateways, this.redisPubClient, this.log);
		Thread threadAliveT = new Thread(listaGatewaysT);
		threadAliveT.start();
	}

	private void getWork() {
		while (true) {
			String recordTrabajoParteJson = null;
			RTrabajoParte recordTrabajoParte = null;
			while (recordTrabajoParteJson == null){
				try {
					recordTrabajoParteJson = Tools.<IWorkerAction>connectRandomGatewayRMI(this.listaGateways, EServicio.WORKER_ACTION, -1, this.log).getWorkToDo(this.workerName);
					if(recordTrabajoParteJson == null) {
						Thread.sleep(1000);
					}
				} catch (InterruptedException | RemoteException e) {
					log.error("Error: " + e.getMessage());
				}
			}
			log.info("==========Trabajo Iniciado=========");
			recordTrabajoParte = gson.fromJson(recordTrabajoParteJson, RTrabajoParteType);
			log.info("Recibi un nuevo trabajo: " + recordTrabajoParte.rParte().uuid());
			File thisWorkDir = new File(this.singleWorkerDir + this.worksDir + recordTrabajoParte.rParte().uuid());
			DirectoryTools.checkOrCreateFolder(thisWorkDir.getAbsolutePath(), this.log);
			File thisWorkRenderDir = new File(thisWorkDir + this.rendersDir);
			DirectoryTools.checkOrCreateFolder(thisWorkRenderDir.getAbsolutePath(), this.log);
			File thisWorkBlendDir = new File(thisWorkDir + this.blendDir);
			DirectoryTools.checkOrCreateFolder(thisWorkBlendDir.getAbsolutePath(), this.log);
			byte[] blendFileBytes = new byte[0];
			try {
				blendFileBytes = Tools.<IWorkerAction>connectRandomGatewayRMI(this.listaGateways, EServicio.WORKER_ACTION, -1, this.log).getBlendFile(recordTrabajoParte.rTrabajo().gStorageBlendName());
			} catch (RemoteException e) {
				log.error("Error: " + e.getMessage());
			}
			File blendFile = persistBlendFile(blendFileBytes, thisWorkBlendDir.getAbsolutePath(), recordTrabajoParte.rTrabajo().gStorageBlendName());
			startRender(recordTrabajoParte, thisWorkRenderDir.getAbsolutePath(), blendFile);
			DirectoryTools.deleteDirectory(thisWorkDir);
		}
	}

	private File persistBlendFile(byte[] byteBlend, String thisWorkBlendDir, String gStorageBlendName) {
		File blend = new File(thisWorkBlendDir + "/" + gStorageBlendName);
		try (FileOutputStream fos = new FileOutputStream(blend)) {
			fos.write(byteBlend);
		} catch (Exception e) {
			log.error("ERROR: " + e.getMessage());
		}
		return blend;
	}

	private void startRender(RTrabajoParte recordTrabajoParte, String thisWorkRenderDir, File blendFile) {
		long startTime = System.currentTimeMillis();
		//Formato: blender -b file_name.blend -f 1 //blender -b file_name.blend -s 1 -e 100 -a
		log.info("Pre-configurando el archivo .blend");
		String cmd;
		int totalFrames = recordTrabajoParte.rParte().endFrame() - recordTrabajoParte.rParte().startFrame();
		int threadsNedeed = 1;
		CountDownLatch latch;
		List<WorkerProcessThread> workerThreads = new ArrayList<>();
		File f0 = new File(thisWorkRenderDir);
		if(totalFrames == 0) {
			if(SystemUtils.IS_OS_WINDOWS){
				cmd = " -b \"" + blendFile.getAbsolutePath() + "\" -o \"" + f0.getAbsolutePath() + "/frame_#####\"" + " -f " + recordTrabajoParte.rParte().startFrame();
			} else {
				cmd = " -b " + blendFile.getAbsolutePath() + " -o " + f0.getAbsolutePath() + "/frame_#####" + " -f " + recordTrabajoParte.rParte().startFrame();
			}
			latch = new CountDownLatch(threadsNedeed);
			File f1 = new File(this.singleWorkerDir + this.blenderExe + cmd);//Normalize backslashs and slashs
			System.out.println("CMD: " + f1.getAbsolutePath());
			workerThreads.add(new WorkerProcessThread(latch, f1.getAbsolutePath(), this.log));
		} else {
			if (totalFrames >= 100) {
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
				if(SystemUtils.IS_OS_WINDOWS){
					cmd = " -b \"" + blendFile.getAbsolutePath() + "\" -o \"" + f0.getAbsolutePath() + "/frame_#####\"" + " -s " + startFrame + " -e " + endFrame + " -a";
				} else {
					cmd = " -b " + blendFile.getAbsolutePath() + " -o " + f0.getAbsolutePath() + "/frame_#####" + " -s " + startFrame + " -e " + endFrame + " -a";
				}
				File f1 = new File(this.singleWorkerDir + this.blenderExe + cmd);//Normalize backslashs and slashs
				log.info("CMD: " + f1.getAbsolutePath());
				workerThreads.add(new WorkerProcessThread(latch, f1.getAbsolutePath(), this.log));
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
			log.error("Error: " + e.getMessage());
		}
		try {
			new ZipFile(thisWorkRenderDir + recordTrabajoParte.rTrabajo().gStorageBlendName()+".zip").addFolder(new File(thisWorkRenderDir));
		} catch (ZipException e) {
			log.error("Error: " + e.getMessage());
		}
		long endTime = System.currentTimeMillis()-startTime;
		log.info("Render All Threads Time Elapsed: " + endTime + "ms");
		try {
			File zipRenderedImages = new File(thisWorkRenderDir + recordTrabajoParte.rTrabajo().gStorageBlendName()+".zip");
			byte[] zipWithRenderedImages = Files.readAllBytes(zipRenderedImages.toPath());
			boolean zipSent = false;
			while(!zipSent) {
				try {
					Tools.<IWorkerAction>connectRandomGatewayRMI(this.listaGateways, EServicio.WORKER_ACTION, -1, this.log).setParteDone(this.workerName, recordTrabajoParte.rParte().uuid(), zipWithRenderedImages);
					zipSent = true;
				} catch (IOException e) {
					log.error("Conexión perdida con el Servidor.");
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		log.info("==========Trabajo Terminado=========");
	}



	private void runRedisPubClient() {
		this.redisPubClient = RedisClient.create(this.redisPubURI);
		StatefulRedisConnection redisConnection = this.redisPubClient.connect();
		log.info("Conectado a Redis Público exitosamente.");
		RedisCommands commands = redisConnection.sync();
		this.listaGateways = gson.fromJson(String.valueOf(commands.hvals("listaGateways")), RListaGatewayType);
		redisConnection.close();
	}

	/*
	 * Verifica si el worker tiene las carpetas necesarias para trabajar,
	 * en caso de no tenerlas las descarga por ftp.
	 */
	private boolean checkNeededFiles() {
		File appDir = new File(this.appDir);
		File workerDir = new File(this.workerDir);
		File singleWorkerDir = new File(this.singleWorkerDir);
		DirectoryTools.checkOrCreateFolder(appDir.getAbsolutePath(), this.log);
		DirectoryTools.checkOrCreateFolder(workerDir.getAbsolutePath(), this.log);
		long sizeWorkerDir = DirectoryTools.getFolderSize(workerDir);
		if (sizeWorkerDir < 30000000) {
			DirectoryTools.checkOrCreateFolder(singleWorkerDir.getAbsolutePath(), this.log);
			long sizeSingleWorkerDir = DirectoryTools.getFolderSize(singleWorkerDir);
			log.info("Obteniendo tamanio de: " + singleWorkerDir.getAbsolutePath() + " MB:" + (sizeSingleWorkerDir / 1024));
			if (sizeSingleWorkerDir < 30000000) {
				downloadBlenderApp();
			}
		} else {
			String contents[] = workerDir.list();
			this.workerName = contents[0];
			log.info("Directorio Detectado -> " + this.workerName);
			this.singleWorkerDir = workerDir + "/" + this.workerName + "/";
		}
		DirectoryTools.checkOrCreateFolder(this.singleWorkerDir + this.worksDir, this.log);
		log.info("Iniciado Worker -> " + this.workerName);
		log.info("Blender ----> LISTO");
		return true;
	}

	private void downloadBlenderApp() {
		log.info("La carpeta BlenderApp esta corrupta o no existe. Descargandola...");
		log.info("Intentando descargar... Porfavor espere, este proceso podria tardar varios minutos...");
		try {
			FileUtils.copyURLToFile(
					new URL(this.urlBlenderPortable),
					//new URL("file:/E:\\Bibliotecas\\Desktop\\blender-3.3.1-windows-x64.zip"),
					//new URL("file:/home/debian/Desktop/blender-3.3.1-linux-x64.tar.xz"),
					new File(this.singleWorkerDir + this.blenderPortable),
					10000,
					10000);
			if(SystemUtils.IS_OS_WINDOWS){
				unzipBlenderPortable();
			} else {
				untarBlenderPortable();
			}
			renameBlenderFolder();
		} catch (IOException e) {
			log.error("Error: " + e.getMessage());
		}
	}

	private void unzipBlenderPortable(){
		log.info("Comenzando a UnZipear Blender... Porfavor espere, este proceso podria tardar varios minutos...");
		try {
			new ZipFile(this.singleWorkerDir + this.blenderPortable).extractAll(this.singleWorkerDir);
		} catch (ZipException e) {
			log.error("Error: " + e.getMessage());
		}
		log.info("Unzip Blender terminado.");
		File zipFileBlender = new File(this.singleWorkerDir + this.blenderPortable);
		zipFileBlender.delete();
	}

	private void untarBlenderPortable(){
		log.info("Comenzando a UnTarear Blender... Porfavor espere, este proceso podria tardar varios minutos...");

		try {
			ProcessBuilder pb = new ProcessBuilder("tar", "-xf", this.singleWorkerDir + this.blenderPortable);
			pb.inheritIO();
			pb.directory(new File(this.singleWorkerDir));
			Process process = pb.start();
			process.waitFor();
		} catch (InterruptedException | IOException e) {
			log.error("Error: " + e.getMessage());
		}
		log.info("Untar Blender terminado.");
		File tarFileBlender = new File(this.singleWorkerDir + this.blenderPortable);
		tarFileBlender.delete();
	}

	private void renameBlenderFolder(){
		File directoryPath = new File(this.singleWorkerDir);
		String contents[] = directoryPath.list();
		File dirToRename = new File(this.singleWorkerDir + contents[0]);
		File newDir = new File(this.singleWorkerDir + this.blenderDir);
		dirToRename.renameTo(newDir);
	}
	private void readConfigFile() {
		Gson gson = new Gson();
		Map config;
		try {
			this.localIp = Inet4Address.getLocalHost().getHostAddress();

			InputStream stream = Worker.class.getClassLoader().getResourceAsStream("Worker/config.json");
			config = gson.fromJson(IOUtils.toString(stream, "UTF-8"), Map.class);

			this.redisPubURI = "redis://"+dotenv.get("REDIS_PUBLIC_USER")+":"+dotenv.get("REDIS_PUBLIC_PASS")+"@"+dotenv.get("REDIS_PUBLIC_IP")+":"+dotenv.get("REDIS_PUBLIC_PORT");

			Map paths = (Map) config.get("paths");
			this.blenderExe = paths.get("blenderExe").toString();
			this.blenderDir = paths.get("blenderDir").toString();
			this.worksDir = paths.get("worksDir").toString();
			this.blendDir = paths.get("blendDir").toString();
			this.rendersDir = paths.get("rendersDir").toString();
			this.blenderPortable = paths.get("blenderPortable").toString();
			if (SystemUtils.IS_OS_WINDOWS) {
				this.urlBlenderPortable = paths.get("urlBlenderPortableWindows").toString();
				this.blenderPortable += ".zip";
			} else {
				this.urlBlenderPortable = paths.get("urlBlenderPortableLinux").toString();
				this.blenderPortable += ".tar.xz";
			}

		} catch (IOException e) {
			log.error("Error Archivo Config!");
		}
	}

	public static void main(String[] args) { new Worker(); }

}
