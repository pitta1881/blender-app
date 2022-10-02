package blender.distributed.Worker;

import blender.distributed.Servidor.FTP.IFTPAction;
import blender.distributed.Servidor.Trabajo.PairTrabajoParte;
import blender.distributed.Servidor.Trabajo.Trabajo;
import blender.distributed.Servidor.Trabajo.TrabajoPart;
import blender.distributed.Servidor.Worker.IWorkerAction;
import blender.distributed.SharedTools.DirectoryTools;
import blender.distributed.Worker.FTP.ClientFTP;
import com.google.gson.Gson;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.nio.file.Files;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class Worker implements Runnable {
	//General
	Logger log = LoggerFactory.getLogger(Worker.class);
	String blenderPortableZip;
	String workerDir = System.getProperty("user.dir") + "\\src\\main\\resources\\Worker\\";
	String workerName = "worker"+System.currentTimeMillis(); //"worker1663802677985"; //"worker1663802677984";
	String singleWorkerDir = workerDir+"\\"+workerName+"\\"; //"\\worker"+System.currentTimeMillis()+"\\";
	String blenderExe;
	String worksDir;
	String blendDir;
	String rendersDir;
	String localIp;
	IWorkerAction stubServer;
	//ftp
	IFTPAction stubFtp;
	ClientFTP cliFtp;
	int serverFTPPort;
	//server
	String serverIp;
	int serverPort;
	private boolean onBackupSv;


	public void startWorker() {
		//while(true) {
		log.info("<-- [STEP 1] - LEYENDO ARCHIVO DE CONFIGURACION \t\t\t-->");
		readConfigFile();
		MDC.put("log.name", Worker.class.getSimpleName() + "-" + this.localIp);
		log.info("<-- [STEP 2] - REALIZANDO CONEXION RMI \t\t\t-->");
		getRMI();
		log.info("<-- [STEP 3] - LANZANDO THREAD ALIVE \t\t\t-->");
		lanzarThread();
		//log.info("<-- [STEP 4] - REALIZANDO CONEXION CON RABBITMQ -->");
		//getQueueConn();
		log.info("<-- [STEP 5] - REVISANDO ARCHIVOS NECESARIOS\t-->");
		if (checkNeededFiles()) {
			log.info("<-- [STEP 6] - ESPERANDO TRABAJOS\t\t\t-->");
			getWork();
		} else {
			log.debug("Error inesperado!");
		}
		//}
	}

	private void lanzarThread() {
		WorkerAliveThread alive = new WorkerAliveThread(this.stubServer, this.workerName);
		Thread tAlive = new Thread(alive);
		tAlive.start();
	}

	private void getWork() {
		PairTrabajoParte pairTP;
		Trabajo trabajo = null;
		TrabajoPart parte = null;
		boolean salir = false;
		DirectoryTools.checkOrCreateFolder(this.worksDir);
		while (!salir) {
			try {
				do {
					pairTP = this.stubServer.giveWorkToDo(this.workerName);
					if(pairTP != null) {
						trabajo = pairTP.trabajo();
						parte = pairTP.parte();
					} else {
						Thread.sleep(1000);
					}
				} while (pairTP == null);
				File thisWorkDir = new File(this.worksDir + trabajo.getBlendName());
				DirectoryTools.checkOrCreateFolder(thisWorkDir.getAbsolutePath());
				File thisWorkRenderDir = new File(thisWorkDir + this.rendersDir);
				DirectoryTools.checkOrCreateFolder(thisWorkRenderDir.getAbsolutePath());
				File thisWorkBlendDir = new File(thisWorkDir + this.blendDir);
				DirectoryTools.checkOrCreateFolder(thisWorkBlendDir.getAbsolutePath());
				log.info("Recibi un nuevo trabajo: " + trabajo.getBlendName());
				log.info("Parte Nº: " + pairTP.parte().getNParte());
				File blendFile = persistBlendFile(trabajo.getBlendFile(), thisWorkBlendDir.getAbsolutePath(), trabajo.getBlendName());
				startRender(trabajo, parte, thisWorkRenderDir.getAbsolutePath(), blendFile);
				DirectoryTools.deleteDirectory(thisWorkDir);
				trabajo = null;
				this.stubServer.checkStatus();
			} catch (RemoteException | InterruptedException e) {
				throw new RuntimeException(e);
			}
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

	private void startRender(Trabajo work, TrabajoPart parte, String thisWorkRenderDir, File blendFile) {
		//Formato: blender -b file_name.blend -f 1 //blender -b file_name.blend -s 1 -e 100 -a
		//First configure default settings to .blend
		log.info("Pre-configurando el archivo .blend");
		String cmd;
		int totalFrames = parte.getEndFrame() - parte.getStartFrame();
		int threadsNedeed = 1;
		CountDownLatch latch;
		List<WorkerProcessThread> workerThreads = new ArrayList<>();
		if(totalFrames == 0) {
			cmd = " -b \"" + blendFile.getAbsolutePath() + "\" -o \"" + thisWorkRenderDir + "\\frame_#####\"" + " -f " + parte.getStartFrame();
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
			int startFrame = parte.getStartFrame();
			int endFrame = startFrame + rangeFrame;
			latch = new CountDownLatch(threadsNedeed);
			for (int i = 0; i < threadsNedeed; i++) {
				cmd = " -b \"" + blendFile.getAbsolutePath() + "\" -o \"" + thisWorkRenderDir + "\\frame_#####\"" + " -s " + startFrame + " -e " + endFrame + " -a";
				File f = new File(this.blenderExe + cmd);//Normalize backslashs and slashs
				log.info("CMD: " + f.getAbsolutePath());
				workerThreads.add(new WorkerProcessThread(latch, f.getAbsolutePath()));
				startFrame = endFrame + 1;
				endFrame += rangeFrame;
				if(endFrame > parte.getEndFrame()){
					endFrame = parte.getEndFrame();
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
			new ZipFile(thisWorkRenderDir + work.getBlendName()+"__part__"+parte.getNParte()+".zip").addFolder(new File(thisWorkRenderDir));
		} catch (ZipException e) {
			throw new RuntimeException(e);
		}
		try {
			File zipRenderedImages = new File(thisWorkRenderDir + work.getBlendName()+"__part__"+parte.getNParte()+".zip");
			try {
				byte[] zipWithRenderedImages = Files.readAllBytes(zipRenderedImages.toPath());
				this.stubServer.setTrabajoParteStatusDone(this.workerName, work.getId(), parte.getNParte(), zipWithRenderedImages);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("==========Termine=========");
	}


	private ClientFTP connectFTP() {
		try {
			log.info("Iniciando servidor FTP.");
			this.serverFTPPort = this.stubFtp.getFTPPort();
			ClientFTP cliFtp = null;
			if (this.stubFtp.startFTPServer() > 0) {
				log.info("El servidor FTP fue iniciado correctamente");
				cliFtp = new ClientFTP(serverIp, serverFTPPort);
			} else {
				boolean recuperado = this.stubFtp.resumeFTPServer();
				log.error("El servidor FTP ya estaba iniciado. Intentando establecer comunicacion: " + recuperado);
				if (recuperado) {
					cliFtp = new ClientFTP(serverIp, serverFTPPort);
				}
			}
			return cliFtp;
		} catch (Exception e) {
			log.error("Hubo un error inesperado al intentar conectarse al servidor FTP.");
			log.error("Error:" + e.getMessage());
			return null;
		}
	}

	private void getRMI() {
		try {
			Registry clienteRMI = LocateRegistry.getRegistry(this.serverIp, serverPort);
			log.info("Obteniendo servicios RMI.");
			log.info("Obteniendo stub...");
			this.stubFtp = (IFTPAction) clienteRMI.lookup("Acciones");
			this.stubServer = (IWorkerAction) clienteRMI.lookup("server");
		} catch (RemoteException | NotBoundException e) {
			log.error("RMI Error: " + e.getMessage());
			if (this.onBackupSv) {
				log.info("Re-intentando conectar al servidor principal: " + this.serverIp + ":" + this.serverPort);
				for (int i = 3; i > 0; i--) {
					try {
						Thread.sleep(1000);
						log.info("Re-intentando en..." + i);
					} catch (InterruptedException e1) {
						log.error(e1.getMessage());
					}
				}
				readConfigFile();//Vuelvo a la config principal
				this.onBackupSv = false;
			} else {
				log.info("Re-intentando conectar al servidor backup: " + this.serverIp + ":" + this.serverPort);
				for (int i = 3; i > 0; i--) {
					try {
						Thread.sleep(1000);
						log.info("Re-intentando en..." + i);
					} catch (InterruptedException e1) {
						log.error(e1.getMessage());
					}
				}
				reconfigWorker();
				this.onBackupSv = true;
			}
			getRMI();
		}
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
		log.info("La carpeta BlenderApp esta corrupta o no existe. Descargandola desde el servidor FTP");
		this.cliFtp = connectFTP();
		try {
			if (this.cliFtp != null) {
				log.info("Intentando descargar...Porfavor espere, este proceso podria tardar varios minutos...");
				this.cliFtp.downloadSingleFile(this.cliFtp.getClient(), "\\" + this.blenderPortableZip, this.singleWorkerDir);
				this.cliFtp.closeConn();
				this.stubFtp.stopFTPServer();
				log.info("Comenzando a UnZipear Blender... Porfavor espere, este proceso podria tardar varios minutos...");
				new ZipFile(this.singleWorkerDir + this.blenderPortableZip).extractAll(this.singleWorkerDir);
				log.info("Unzip Blender terminado.");
				File zipFileBlender = new File(this.singleWorkerDir + this.blenderPortableZip);
				zipFileBlender.delete();
			} else {
				log.error("Hubo un problema con el servidor FTP");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void readConfigFile() {
		Gson gson = new Gson();
		Map config;
		try {
			this.localIp = Inet4Address.getLocalHost().getHostAddress();
			config = gson.fromJson(new FileReader(this.workerDir + "config.json"), Map.class);

			Map server = (Map) config.get("server");
			this.serverIp = server.get("ip").toString();
			this.serverPort = Integer.valueOf(server.get("port").toString());
			this.serverFTPPort = Integer.valueOf(server.get("ftp").toString());

			Map paths = (Map) config.get("paths");
			this.blenderExe = this.singleWorkerDir + paths.get("blenderExe");
			this.worksDir = this.singleWorkerDir + paths.get("worksDir");
			this.blendDir = paths.get("blendDir").toString();
			this.rendersDir = paths.get("rendersDir").toString();
			this.blenderPortableZip = paths.get("blenderPortableZip").toString();

			this.onBackupSv = false;
		} catch (IOException e) {
			log.info("Error Archivo Config!");
		}
	}

	private void reconfigWorker() {
		Gson gson = new Gson();
		Map config;
		try {
			config = gson.fromJson(new FileReader(this.workerDir + "config.json"), Map.class);
			Map server = (Map) config.get("server");
			this.serverIp = server.get("ipBak").toString();

			this.onBackupSv = true;
		} catch (IOException e) {
			log.info("Error Archivo Config!");
		}
	}

	public static void main(String[] args) {
		Worker wk = new Worker();
		wk.startWorker();
	}

	@Override
	public void run() {
		startWorker();
	}

}
