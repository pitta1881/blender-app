package blender.distributed.Worker;

import blender.distributed.Servidor.IFTPManager;
import blender.distributed.Servidor.IWorkerAction;
import blender.distributed.Servidor.Mensaje;
import blender.distributed.Worker.Tools.ClientFTP;
import blender.distributed.Worker.Tools.DirectoryTools;
import com.google.gson.Gson;
import net.lingala.zip4j.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Inet4Address;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Map;


/*TODO
 *
 *
 *  #Por linea de comandos realizar un renderizado y subirlo a la cola
 *
 */
public class Worker implements Runnable {
	//General
	Logger log = LoggerFactory.getLogger(Worker.class);
	String workerDirectory = System.getProperty("user.dir") + "\\src\\main\\resources\\Worker\\";

	URL workerConfigFile = this.getClass().getClassLoader().getResource("Worker\\config.json");
	String myBlendDirectory;
	String myBlenderApp;
	String blenderPortableZip;
	String myRenderedImages;
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
	String singleWorkerWorkspace = "worker"+System.currentTimeMillis(); //"worker1663802677984"


	public void startWorker() {
		//while(true) {
		log.info("<-- [STEP 1] - LEYENDO ARCHIVO DE CONFIGURACION \t\t\t-->");
		readConfigFile();
		MDC.put("log.name", Worker.class.getSimpleName().toString() + "-" + this.localIp);
		log.info("<-- [STEP 3] - REALIZANDO CONEXION RMI \t\t\t-->");
		getRMI();
		log.info("<-- [STEP 4] - LANZANDO THREAD ALIVE \t\t\t-->");
		lanzarThread();
		//log.info("<-- [STEP 5] - REALIZANDO CONEXION CON RABBITMQ -->");
		//getQueueConn();
		log.info("<-- [STEP 6] - REVISANDO ARCHIVOS NECESARIOS\t-->");
		if (checkNeededFiles()) {
			log.info("<-- [STEP 7] - ESPERANDO TRABAJOS\t\t\t-->");
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
		Mensaje trabajo = new Mensaje("");
		boolean salir = false;
		while (!salir) {
			try {
				while (trabajo.getName().length() < 1) {
					trabajo = this.stubServer.giveWorkToDo(localIp);
					if (trabajo.getName().contentEquals("empty")) {
						trabajo = new Mensaje("");
					}
					Thread.sleep(1000);
				}
				System.out.println(trabajo.getStatus());
				log.debug("Recibi un nuevo trabajo: " + trabajo.getName());
				persistBlendFile(trabajo.getBlend(), trabajo.getName());
				startRender(trabajo);
				borrarTemporales();
				trabajo = new Mensaje("");
				this.stubServer.checkStatus();
			} catch (RemoteException | InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}


	private void persistBlendFile(byte[] byteBlend, String name) {
		DirectoryTools.checkOrCreateFolder(this.myBlendDirectory);
		File folder = new File(this.myBlendDirectory);
		File blend = new File(folder.getAbsolutePath() + "/" + name);
		try (FileOutputStream fos = new FileOutputStream(blend)) {
			fos.write(byteBlend);
		} catch (Exception e) {
			log.error("ERROR: " + e.getMessage());
		}
	}

	private void startRender(Mensaje trabajo) {
		//Formato: blender -b file_name.blend -f 1 //blender -b file_name.blend -s 1 -e 100 -a
		int i = 1;
		String blendToRender = this.myBlendDirectory + "/" + getFiles(this.myBlendDirectory).get(0);
		File finishedWorkFolder = new File(this.myRenderedImages);
		DirectoryTools.checkOrCreateFolder(this.myRenderedImages);
		//First configure default settings to .blend
		log.info("Pre-configurando el archivo .blend");
		String cmd = " -b \"" + blendToRender + "\" -o \""+this.myRenderedImages+"/frame_#####\""+ " -f " + trabajo.getStartFrame();
		File f = new File(this.myBlenderApp + cmd);//Normalize backslashs and slashs

		System.out.println("CMD: " + f.getPath());
		ejecutar(f.getPath());
		//Start render
		ArrayList<String> imgTerminadas = getFiles(finishedWorkFolder.getPath());
		try {
			File imgRendered = new File(finishedWorkFolder.getPath() + "/" + imgTerminadas.get(imgTerminadas.size() - 1));
			BufferedImage image = ImageIO.read(imgRendered);
			trabajo.setRenderedImage(image);
			this.stubServer.setTrabajoStatusDone(trabajo);
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("==========Termine=========");
	}


	public ArrayList<String> getFiles(String path) {
		File f = new File(path);
		if (f.isDirectory()) {
			ArrayList<String> res = new ArrayList<String>();
			File[] arr_content = f.listFiles();
			int size = arr_content.length;
			for (int i = 0; i < size; i++) {
				if (arr_content[i].isFile())
					res.add(arr_content[i].getName());
			}
			return res;
		} else {
			f.mkdir();
			return null;
		}
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
		String pathRoot = this.workerDirectory;
		String pathApp = this.workerDirectory + "\\Blender-app\\";
		String pathWorkerApp = this.workerDirectory + "\\Blender-app\\" + singleWorkerWorkspace;
		File fWApp = new File(pathWorkerApp);
		DirectoryTools.checkOrCreateFolder(pathRoot);
		DirectoryTools.checkOrCreateFolder(pathApp);
		DirectoryTools.checkOrCreateFolder(pathWorkerApp);
		long size = DirectoryTools.getFolderSize(fWApp);
		log.info("Obteniendo tamanio de: " + fWApp.getAbsolutePath() + " MB:" + (size / 1024));
		if (size < 30000000) {
			downloadBlenderApp(fWApp.getAbsolutePath());
		} else {
			log.info("Blender ----> LISTO");
		}
		return true;
	}

	@SuppressWarnings("static-access")
	private void downloadBlenderApp(String myAppDir) {
		log.info("La carpeta BlenderApp esta corrupta o no existe. Descargandola desde el servidor FTP");
		this.cliFtp = connectFTP();
		try {
			if (this.cliFtp != null) {
				log.info("Intentando descargar...Porfavor espere, este proceso podria tardar varios minutos...");
				this.cliFtp.downloadSingleFile(this.cliFtp.getClient(), "/" + this.blenderPortableZip, myAppDir);
				this.cliFtp.closeConn();
				this.stubFtp.stopFTPServer();
				log.info("Comenzando a UnZipear Blender... Porfavor espere, este proceso podria tardar varios minutos...");
				new ZipFile(myAppDir + "/" + this.blenderPortableZip).extractAll(myAppDir);
				log.info("Unzip terminado.");
				File zipFileBlender = new File(myAppDir + "/" + this.blenderPortableZip);
				zipFileBlender.delete();
			} else {
				log.error("Hubo un problema con el servidor FTP");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("rawtypes")
	private void readConfigFile() {
		Gson gson = new Gson();
		Map config;
		try {
			this.localIp = Inet4Address.getLocalHost().getHostAddress();
			config = gson.fromJson(new FileReader(this.workerDirectory + "config.json"), Map.class);

			Map server = (Map) config.get("server");
			this.serverIp = server.get("ip").toString();
			this.serverPort = Integer.valueOf(server.get("port").toString());
			this.serverFTPPort = Integer.valueOf(server.get("ftp").toString());

			Map paths = (Map) config.get("paths");
			this.myBlendDirectory = this.workerDirectory + "\\Blender-app\\" + singleWorkerWorkspace + paths.get("myBlendDir").toString();
			this.myBlenderApp = this.workerDirectory + "\\Blender-app\\" + singleWorkerWorkspace + paths.get("myBlenderApp");
			this.blenderPortableZip = paths.get("blenderPortableZip").toString();
			this.myRenderedImages = this.workerDirectory + "\\Blender-app\\" + singleWorkerWorkspace + paths.get("myFinishedWorks");

			this.onBackupSv = false;
		} catch (IOException e) {
			log.info("Error Archivo Config!");
		}
	}

	@SuppressWarnings("rawtypes")
	private void reconfigWorker() {
		Gson gson = new Gson();
		Map config;
		try {
			config = gson.fromJson(new FileReader(this.workerDirectory + "config.json"), Map.class);
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

	private void borrarTemporales() {
		for(String toErase : getFiles(myBlendDirectory)) {
			File f = new File(myBlendDirectory+"/"+toErase);
			if(f.delete()){
				System.out.println(myBlendDirectory+toErase+" -> Eliminado.");
			}else {
				System.out.println(myBlendDirectory+toErase+" -> No existe, o no se puede eliminar.");
			}
		}
		for(String toErase : getFiles(myRenderedImages)) {
			File f = new File(myRenderedImages+"/"+toErase);
			if(f.delete()){
				System.out.println(myRenderedImages+toErase+" -> Eliminado.");
			}else {
				System.out.println(myRenderedImages+toErase+" -> No existe, o no se puede eliminar.");
			}
		}
	}
}
