package blender.distributed.Cliente;

import blender.distributed.Servidor.IClientAction;
import blender.distributed.Servidor.Mensaje;
import blender.distributed.Worker.Tools.DirectoryTools;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;

public class Cliente{
	private final int MAX_ATTEMPS = 3;
	static Logger log = LoggerFactory.getLogger(Cliente.class);
	IClientAction stub;
	File file;
	byte[] fileContent;
	private String serverIp;
	private String serverIpBak;
	private Integer serverPort;
	private boolean onBackupSv = false;
	String clienteDirectory = System.getProperty("user.dir")+"\\src\\main\\resources\\Cliente\\";
	String myRenderedImages;

	public Cliente() {
		readConfigFile();
		MDC.put("log.name", Cliente.class.getSimpleName().toString());
	}
	
	public void connectRMI(String serverIp, int serverPort, int attemps) {
		Registry clienteRMI;
		try {
			clienteRMI = LocateRegistry.getRegistry(serverIp, serverPort);
			this.stub = (IClientAction) clienteRMI.lookup("client");
			String myIp = "";
			String myHostName = "";
			try {
				myIp = Inet4Address.getLocalHost().getHostAddress();
				myHostName = Inet4Address.getLocalHost().getCanonicalHostName();
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
			}
			String servResp = this.stub.helloServer(myIp, myHostName);
			if(!servResp.isEmpty()) {
				log.info("Conectado al Servidor: " + serverIp + ":" + serverPort);
			}
		} catch (RemoteException | NotBoundException e) {
			log.error("No se pudo conectar al servidor.Reintentando en 5 segundos");
			if(attemps > this.MAX_ATTEMPS) {
				if(!onBackupSv) {
					try {
						attemps = 0;
						log.info("Parece que el servidor principal esta caído, intentando conectar con el de respaldo...");
						Thread.sleep(2000);
						log.info("Intentando reconectar...");
						this.onBackupSv = true;
						connectRMI(this.serverIpBak, serverPort, ++attemps);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}else {
					try {
						attemps = 0;
						log.info("Parece que el servidor de respaldo esta caído, intentando conectar con el servidor principal...");
						Thread.sleep(2000);
						log.info("Intentando reconectar...");
						this.onBackupSv = false;
						connectRMI(this.serverIp, serverPort, ++attemps);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
			}else {
				try {
					Thread.sleep(2000);
					log.info("Intentando reconectar...");
					connectRMI(serverIp, serverPort, ++attemps);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
	public void setFile(File f) {
		try {
			this.file = f;
			this.fileContent = Files.readAllBytes(file.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public String enviarFile(int startFrame, int endFrame) {
		connectRMI(serverIp, serverPort, 0);
		if(this.file != null) {
			log.info("Enviando el archivo: "+this.file.getName());
			String myIp = "";
			try {
				myIp = Inet4Address.getLocalHost().getHostAddress();
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
			}
			Mensaje m = new Mensaje(this.fileContent, file.getName(), startFrame, endFrame, myIp);
			try {
				byte[] zipReturnedBytes = this.stub.renderRequest(m);
				if(zipReturnedBytes.length < 100) {
					return "Ha ocurrido un error. Porfavor intentelo denuevo mas tarde";
				}
				DirectoryTools.checkOrCreateFolder(this.myRenderedImages);
				File zipResult = new File(this.myRenderedImages + "\\"+file.getName()+".zip");
				try (FileOutputStream fos = new FileOutputStream(zipResult)) {
					fos.write(zipReturnedBytes);
					log.info("Exito: Archivo "+file.getName()+".zip recibido!");
				} catch (Exception e) {
					log.error("ERROR: " + e.getMessage());
				}
				return zipResult.getAbsolutePath();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}else {
			log.error("Error: Archivo no cargado");
			return "Error";
		}
		return "Error";
	}
	public boolean isReady() {
		if(this.file != null && this.fileContent != null)
			return true;
		else return false;
	}
	
	private void readConfigFile() {
		Gson gson = new Gson();
		Map config;
		try {
			config = gson.fromJson(new FileReader(this.clienteDirectory+"config.json"), Map.class);
			Map server = (Map) config.get("server");
			Map paths = (Map) config.get("paths");
			this.serverIp = server.get("ip").toString();
			this.serverIpBak = server.get("ipBak").toString();
			this.serverPort = Integer.valueOf(server.get("port").toString());
			this.myRenderedImages = this.clienteDirectory + paths.get("renderedImages");

		} catch (IOException e) {
			log.info("Error Archivo Config!");
		} 
	}

}

