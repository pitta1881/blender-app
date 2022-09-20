package blender.distributed.Worker;

import blender.distributed.Servidor.IFTPManager;
import blender.distributed.Servidor.IWorkerAction;
import blender.distributed.Worker.Tools.ClientFTP;
import blender.distributed.Worker.Tools.DirectoryTools;
import com.google.gson.Gson;
import net.lingala.zip4j.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
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
	String workerDirectory = System.getProperty("user.dir")+"\\src\\main\\resources\\Worker\\";

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
	private ArrayList<String> realizedWorks = new ArrayList<String>();
	private boolean onBackupSv;


	public void startWorker() {
		//while(true) {
			log.info("<-- [STEP 1] - LEYENDO ARCHIVO DE CONFIGURACION \t\t\t-->");
			readConfigFile();
			MDC.put("log.name", Worker.class.getSimpleName().toString()+"-"+this.localIp);
			log.info("<-- [STEP 3] - REALIZANDO CONEXION RMI \t\t\t-->");
			getRMI();
			log.info("<-- [STEP 4] - LANZANDO THREAD ALIVE \t\t\t-->");
			lanzarThread();
			//log.info("<-- [STEP 5] - REALIZANDO CONEXION CON RABBITMQ -->");
			//getQueueConn();
			log.info("<-- [STEP 6] - REVISANDO ARCHIVOS NECESARIOS\t-->");
			if(checkNeededFiles()) {
				log.info("<-- [STEP 7] - ESPERANDO TRABAJOS\t\t\t-->");
				//getWork();
			}else {
				log.debug("Error inesperado!");
			}
		//}
	}

	private void lanzarThread() {
		WorkerAliveThread alive = new WorkerAliveThread(this.stubServer, this.localIp);
		Thread tAlive = new Thread(alive);
		tAlive.start();
	}


	private void persistBlendFile(byte[] byteBlend, String name) {
		File folder = new File(this.myBlendDirectory);
		if(folder.exists() && folder.isDirectory()) {
			File blend = new File(folder.getAbsolutePath()+"/"+name);
			try (FileOutputStream fos = new FileOutputStream(blend)) {
				fos.write(byteBlend);
			}catch (Exception e) {
				log.error("ERROR: "+e.getMessage());
			}
		}else {
			folder.mkdir();
			File blend = new File(folder.getAbsolutePath()+"/"+name);
			try (FileOutputStream fos = new FileOutputStream(blend)) {
				fos.write(byteBlend);
			}catch (Exception e) {
				log.error("ERROR: "+e.getMessage());
			}
		}
		try {
			// TODO Buscar otra forma de asegurarse que el archivo esta completamente listo para ser modificado.
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}



	public ArrayList<String> getFiles( String path ) {
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
			if(this.stubFtp.startFTPServer() > 0) {
				log.info("El servidor FTP fue iniciado correctamente");
				cliFtp = new ClientFTP(serverIp, serverFTPPort);
			}else{
				boolean recuperado = this.stubFtp.resumeFTPServer();
				log.error("El servidor FTP ya estaba iniciado. Intentando establecer comunicacion: "+recuperado);
				if(recuperado) {
					cliFtp = new ClientFTP(serverIp, serverFTPPort);
				}
			}
			return cliFtp;
		} catch (Exception e) {
			log.error("Hubo un error inesperado al intentar conectarse al servidor FTP.");
			log.error("Error:"+e.getMessage());
			return null;
		}
	}

	private void getRMI() {
		try {
			Registry clienteRMI = LocateRegistry.getRegistry(this.serverIp,serverPort);
			log.info("Obteniendo servicios RMI.");
			log.info("Obteniendo stub...");
			this.stubFtp = (IFTPManager) clienteRMI.lookup("Acciones");
			this.stubServer = (IWorkerAction) clienteRMI.lookup("server");
			this.stubServer.helloServer(localIp);
		} catch (RemoteException | NotBoundException e) {
			log.error("RMI Error: "+e.getMessage());
			if(this.onBackupSv) {
				log.info("Re-intentando conectar al servidor principal: "+this.serverIp+":"+this.serverPort);
				for (int i = 5; i > 0; i--) {
					try {
						Thread.sleep(1000);
						log.info("Re-intentando en..."+i);
					} catch (InterruptedException e1) {
						log.error(e1.getMessage());
					}
				}
				readConfigFile();//Vuelvo a la config principal
				this.onBackupSv = false;
				getRMI();
			}else {
				log.info("Re-intentando conectar al servidor backup: "+this.serverIp+":"+this.serverPort);
				for (int i = 5; i > 0; i--) {
					try {
						Thread.sleep(1000);
						log.info("Re-intentando en..."+i);
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
		Long singleWorkerWorkspace = System.currentTimeMillis();
		String pathRoot = this.workerDirectory;
		String pathApp = this.workerDirectory+"\\Blender-app\\";
		String pathWorkerApp = this.workerDirectory+"\\Blender-app\\"+"worker"+singleWorkerWorkspace;
		File fWApp = new File(pathWorkerApp);
		DirectoryTools.checkOrCreateFolder(pathRoot);
		DirectoryTools.checkOrCreateFolder(pathApp);
		DirectoryTools.checkOrCreateFolder(pathWorkerApp);
		long size = DirectoryTools.getFolderSize(fWApp);
		log.info("Obteniendo tamanio de: "+fWApp.getAbsolutePath()+" MB:"+(size/1024));
		if(size < 30000000) {
			downloadBlenderApp(fWApp.getAbsolutePath());
		}else {
			log.info("Blender ----> LISTO");
		}
		return true;
	}

	@SuppressWarnings("static-access")
	private void downloadBlenderApp(String myAppDir) {
		log.info("La carpeta BlenderApp esta corrupta o no existe. Descargandola desde el servidor FTP");
		this.cliFtp = connectFTP();
		try {
			if(this.cliFtp != null) {
				log.info("Intentando descargar...Porfavor espere, este proceso podria tardar varios minutos...");
				this.cliFtp.downloadSingleFile(this.cliFtp.getClient(), "/"+this.blenderPortableZip, myAppDir);
				this.cliFtp.closeConn();
				this.stubFtp.stopFTPServer();
				log.info("Comenzando a UnZipear Blender... Porfavor espere, este proceso podria tardar varios minutos...");
				new ZipFile(myAppDir+"/"+this.blenderPortableZip).extractAll(myAppDir);
				log.info("Unzip terminado.");
				File zipFileBlender= new File(myAppDir+"/"+this.blenderPortableZip);
				zipFileBlender.delete();
			}else {
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
			config = gson.fromJson(new FileReader(this.workerDirectory+"config.json"), Map.class);

			Map server = (Map) config.get("server");
			this.serverIp = server.get("ip").toString();
			this.serverPort = Integer.valueOf(server.get("port").toString());
			this.serverFTPPort = Integer.valueOf(server.get("ftp").toString());

			Map paths = (Map) config.get("paths");
			this.myBlendDirectory = this.workerDirectory + paths.get("myBlendDir").toString();
			this.myBlenderApp = this.workerDirectory + paths.get("myBlenderApp");
			this.myRenderedImages = this.workerDirectory + paths.get("myFinishedWorks");
			this.blenderPortableZip = paths.get("blenderPortableZip").toString();

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
			config = gson.fromJson(new FileReader(this.workerDirectory+"config.json"), Map.class);
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
