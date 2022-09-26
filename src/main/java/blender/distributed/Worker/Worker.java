package blender.distributed.Worker;

import blender.distributed.Servidor.IFTPManager;
import blender.distributed.Servidor.IWorkerAction;
import blender.distributed.Servidor.Trabajo;
import blender.distributed.Worker.Tools.ClientFTP;
import blender.distributed.Worker.Tools.DirectoryTools;
import com.google.gson.Gson;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.*;
import java.net.Inet4Address;
import java.nio.file.Files;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;


public class Worker implements Runnable {
	//General
	Logger log = LoggerFactory.getLogger(Worker.class);
	String blenderPortableZip;
	String workerDir = System.getProperty("user.dir") + "\\src\\main\\resources\\Worker\\";
	String singleWorkerDir = workerDir+"\\worker1663802677984\\"; //"\\worker"+System.currentTimeMillis()+"\\";
	String blenderExe;
	String worksDir;
	String blendDir;
	String rendersDir;
	String localIp;
	IWorkerAction stubServer;
	//ftp
	IFTPManager stubFtp;
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
		WorkerAliveThread alive = new WorkerAliveThread(this.stubServer, this.localIp);
		Thread tAlive = new Thread(alive);
		tAlive.start();
	}

	private void getWork() {
		Trabajo trabajo = null;
		boolean salir = false;
		DirectoryTools.checkOrCreateFolder(this.worksDir);
		while (!salir) {
			try {
				while (trabajo == null) {
					trabajo = this.stubServer.giveWorkToDo(localIp);
					Thread.sleep(1000);
				}
				System.out.println(trabajo.getStatus());
				File thisWorkDir = new File(this.worksDir + trabajo.getBlendName());
				DirectoryTools.checkOrCreateFolder(thisWorkDir.getAbsolutePath());
				File thisWorkRenderDir = new File(thisWorkDir + this.rendersDir);
				DirectoryTools.checkOrCreateFolder(thisWorkRenderDir.getAbsolutePath());
				File thisWorkBlendDir = new File(thisWorkDir + this.blendDir);
				DirectoryTools.checkOrCreateFolder(thisWorkBlendDir.getAbsolutePath());
				log.info("Recibi un nuevo trabajo: " + trabajo.getBlendName());
				File blendFile = persistBlendFile(trabajo.getBlendFile(), thisWorkBlendDir.getAbsolutePath(), trabajo.getBlendName());
				startRender(trabajo, thisWorkRenderDir.getAbsolutePath(), blendFile);
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

	private void startRender(Trabajo work, String thisWorkRenderDir, File blendFile) {
		//Formato: blender -b file_name.blend -f 1 //blender -b file_name.blend -s 1 -e 100 -a
		//First configure default settings to .blend
		log.info("Pre-configurando el archivo .blend");
		String cmd;
		if(work.getStartFrame() == work.getEndFrame()) {
			cmd = " -b \"" + blendFile.getAbsolutePath() + "\" -o \"" + thisWorkRenderDir + "\\frame_#####\"" + " -f " + work.getStartFrame();
		} else {
			cmd = " -b \"" + blendFile.getAbsolutePath() + "\" -o \"" + thisWorkRenderDir + "\\frame_#####\"" + " -s " + work.getStartFrame() + " -e " + work.getEndFrame() + " -a";
		}
		File f = new File(this.blenderExe + cmd);//Normalize backslashs and slashs

		System.out.println("CMD: " + f.getAbsolutePath());
		ejecutar(f.getAbsolutePath());
		try {
			new ZipFile(thisWorkRenderDir + work.getBlendName()+".zip").addFolder(new File(thisWorkRenderDir));
		} catch (ZipException e) {
			throw new RuntimeException(e);
		}
		try {
			File zipRenderedImages = new File(thisWorkRenderDir + work.getBlendName()+".zip");
			try {
				byte[] zipWithRenderedImages = Files.readAllBytes(zipRenderedImages.toPath());
				this.stubServer.setTrabajoStatusDone(work.getId(), zipWithRenderedImages);
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
			this.stubFtp = (IFTPManager) clienteRMI.lookup("Acciones");
			this.stubServer = (IWorkerAction) clienteRMI.lookup("server");
			this.stubServer.helloServer(localIp);
		} catch (RemoteException | NotBoundException e) {
			log.error("RMI Error: " + e.getMessage());
			if (this.onBackupSv) {
				log.info("Re-intentando conectar al servidor principal: " + this.serverIp + ":" + this.serverPort);
				for (int i = 5; i > 0; i--) {
					try {
						Thread.sleep(1000);
						log.info("Re-intentando en..." + i);
					} catch (InterruptedException e1) {
						log.error(e1.getMessage());
					}
				}
				readConfigFile();//Vuelvo a la config principal
				this.onBackupSv = false;
				getRMI();
			} else {
				log.info("Re-intentando conectar al servidor backup: " + this.serverIp + ":" + this.serverPort);
				for (int i = 5; i > 0; i--) {
					try {
						Thread.sleep(1000);
						log.info("Re-intentando en..." + i);
					} catch (InterruptedException e1) {
						log.error(e1.getMessage());
					}
				}
				reconfigWorker();
				this.onBackupSv = true;
				getRMI();
			}
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

	private void ejecutar(String cmd) {
		try {
			Process p = Runtime.getRuntime().exec(cmd);
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = "";
			while (true) {
				line = input.readLine();
				if (line == null) break;
				if (line.contains("| Rendered ")) {

				} else {
					System.out.println("Line: " + line);
				}
			}
			p.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
