package blender.distributed.Cliente;

import blender.distributed.Servidor.Cliente.IClientAction;
import blender.distributed.Servidor.Trabajo.Trabajo;
import blender.distributed.SharedTools.DirectoryTools;
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
	static Logger log = LoggerFactory.getLogger(Cliente.class);
	IClientAction stubServer;
	File file;
	byte[] fileContent;
	private String serverIpMain;
	private String serverIpBak;
	private Integer serverPort;
	private boolean useBackupServer = false;
	String clienteDirectory = System.getProperty("user.dir")+"\\src\\main\\resources\\Cliente\\";
	String myRenderedImages;

	public Cliente() {
		readConfigFile();
		MDC.put("log.name", Cliente.class.getSimpleName());
	}
	
	public void connectRMI(String serverIp, int serverPort) {
		try {
			Registry clienteRMI = LocateRegistry.getRegistry(serverIp, serverPort);
			this.stubServer = (IClientAction) clienteRMI.lookup("client");
			String myIp = "";
			String myHostName = "";
			try {
				myIp = Inet4Address.getLocalHost().getHostAddress();
				myHostName = Inet4Address.getLocalHost().getCanonicalHostName();
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
			}
			String servResp = this.stubServer.helloServer(myIp, myHostName);
			if(!servResp.isEmpty()) {
				log.info("Conectado al Servidor: " + serverIp + ":" + serverPort);
			}
		} catch (RemoteException | NotBoundException e) {
			manageServerFall(e.getMessage());
			connectRMI(useBackupServer ? this.serverIpBak : this.serverIpMain, serverPort);
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
		connectRMI(useBackupServer ? this.serverIpBak : this.serverIpMain, serverPort);
		if(this.file != null) {
			log.info("Enviando el archivo: "+this.file.getName());
			Trabajo work = new Trabajo(this.fileContent, file.getName(), startFrame, endFrame);
			try {
				byte[] zipReturnedBytes = this.stubServer.renderRequest(work);
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
				log.error("Error: " + e.getMessage());
				return "Ha ocurrido un error con la conexión al servidor.";
			}
		}else {
			log.error("Error: Archivo no cargado");
			return "Error";
		}
	}
	public boolean isReady() {
		return (this.file != null && this.fileContent != null);
	}
	
	private void readConfigFile() {
		Gson gson = new Gson();
		Map config;
		try {
			config = gson.fromJson(new FileReader(this.clienteDirectory+"config.json"), Map.class);

			Map server = (Map) config.get("server");
			this.serverIpMain = server.get("ip").toString();
			this.serverIpBak = server.get("ipBak").toString();
			this.serverPort = Integer.valueOf(server.get("port").toString());

			Map paths = (Map) config.get("paths");
			this.myRenderedImages = this.clienteDirectory + paths.get("renderedImages");

		} catch (IOException e) {
			log.error("Error Archivo Config!");
		} 
	}

	private void manageServerFall(String errorMessage){
		log.error("RMI Error: " + errorMessage);
		if (this.useBackupServer) {
			log.info("Re-intentando conectar al servidor backup: " + this.serverIpBak + ":" + this.serverPort);
		} else {
			log.info("Re-intentando conectar al servidor principal: " + this.serverIpMain + ":" + this.serverPort);
		}
		for (int i = 3; i > 0; i--) {
			try {
				Thread.sleep(1000);
				log.info("Re-intentando en..." + i);
			} catch (InterruptedException e1) {
				log.error(e1.getMessage());
			}
		}
		this.useBackupServer = !this.useBackupServer;
	}

}

