package blender.distributed.Worker;

import blender.distributed.Records.RGateway;
import blender.distributed.Records.RTrabajoParte;
import blender.distributed.SharedTools.DirectoryTools;
import blender.distributed.SharedTools.RefreshListaGatewaysThread;
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
import org.slf4j.MDC;

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

import static blender.distributed.Worker.Tools.connectRandomGatewayRMI;


public class Worker {
	//General
	Logger log = LoggerFactory.getLogger(this.getClass());
	String blenderPortable;
	String appDir = System.getProperty("user.dir") + "/app/";
	String workerDir = appDir + "Worker/";
	String workerName = "worker"+System.currentTimeMillis(); //"worker1670282810869"; //"worker1663802677985"; //"worker"+System.currentTimeMillis();
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
			while (recordTrabajoParteJson == null){
				try {
					recordTrabajoParteJson = connectRandomGatewayRMI(this.listaGateways).getWorkToDo(this.workerName);
					if(recordTrabajoParteJson == null) {
						Thread.sleep(1000);
					}
				} catch (InterruptedException | RemoteException e) {
					log.error("Error: " + e.getMessage());
				}
			}
			recordTrabajoParte = gson.fromJson(recordTrabajoParteJson, RTrabajoParteType);
			File thisWorkDir = new File(this.worksDir + recordTrabajoParte.rParte().uuid());
			DirectoryTools.checkOrCreateFolder(thisWorkDir.getPath());
			File thisWorkRenderDir = new File(thisWorkDir + this.rendersDir);
			DirectoryTools.checkOrCreateFolder(thisWorkRenderDir.getPath());
			File thisWorkBlendDir = new File(thisWorkDir + this.blendDir);
			DirectoryTools.checkOrCreateFolder(thisWorkBlendDir.getPath());
			log.info("Recibi un nuevo trabajo: " + recordTrabajoParte.rParte().uuid());
			byte[] blendFileBytes = new byte[0];
			try {
				blendFileBytes = connectRandomGatewayRMI(this.listaGateways).getBlendFile(recordTrabajoParte.rTrabajo().gStorageBlendName());
			} catch (RemoteException e) {
				log.error("Error: " + e.getMessage());
			}
			File blendFile = persistBlendFile(blendFileBytes, thisWorkBlendDir.getPath(), recordTrabajoParte.rTrabajo().gStorageBlendName());
			startRender(recordTrabajoParte, thisWorkRenderDir.getPath(), blendFile);
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
		//Formato: blender -b file_name.blend -f 1 //blender -b file_name.blend -s 1 -e 100 -a
		log.info("Pre-configurando el archivo .blend");
		String cmd;
		int totalFrames = recordTrabajoParte.rParte().endFrame() - recordTrabajoParte.rParte().startFrame();
		int threadsNedeed = 1;
		CountDownLatch latch;
		List<WorkerProcessThread> workerThreads = new ArrayList<>();
		if(totalFrames == 0) {
			cmd = " -b \"" + blendFile.getPath() + "\" -o \"" + thisWorkRenderDir + "/frame_#####\"" + " -f " + recordTrabajoParte.rParte().startFrame();
			latch = new CountDownLatch(threadsNedeed);
			File f = new File(this.blenderExe + cmd);//Normalize backslashs and slashs
			System.out.println("CMD: " + f.getPath());
			workerThreads.add(new WorkerProcessThread(latch, f.getPath()));
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
				cmd = " -b \"" + blendFile.getPath() + "\" -o \"" + thisWorkRenderDir + "/frame_#####\"" + " -s " + startFrame + " -e " + endFrame + " -a";
				File f = new File(this.blenderExe + cmd);//Normalize backslashs and slashs
				log.info("CMD: " + f.getPath());
				workerThreads.add(new WorkerProcessThread(latch, f.getPath()));
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
		try {
			File zipRenderedImages = new File(thisWorkRenderDir + recordTrabajoParte.rTrabajo().gStorageBlendName()+".zip");
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
			log.error(e.getMessage());
		}
		log.info("==========Termine=========");
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
		DirectoryTools.checkOrCreateFolder(appDir.getPath());
		DirectoryTools.checkOrCreateFolder(workerDir.getPath());
		DirectoryTools.checkOrCreateFolder(singleWorkerDir.getPath());
		long size = DirectoryTools.getFolderSize(singleWorkerDir);
		log.info("Obteniendo tamanio de: " + singleWorkerDir.getPath() + " MB:" + (size / 1024));
		if (size < 30000000) {
			downloadBlenderApp();
		} else {
			log.info("Blender ----> LISTO");
		}
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
		File newDir = new File(this.blenderDir);
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
			this.blenderExe = this.singleWorkerDir + paths.get("blenderExe");
			this.blenderDir = this.singleWorkerDir + paths.get("blenderDir");
			this.worksDir = this.singleWorkerDir + paths.get("worksDir");
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
