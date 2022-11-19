package blender.distributed.Cliente;

import blender.distributed.Gateway.Servidor.IGatewayClientAction;
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

import static blender.distributed.SharedTools.Tools.manageGatewayFall;

public class Cliente{
	static Logger log = LoggerFactory.getLogger(Cliente.class);
	IGatewayClientAction stubGateway;
	File file;
	byte[] fileContent;
	private String gatewayIp;
	private int gatewayPort;
	String clienteDirectory = System.getProperty("user.dir")+"\\src\\main\\resources\\Cliente\\";
	String myRenderedImages;

	public Cliente() {
		readConfigFile();
		MDC.put("log.name", Cliente.class.getSimpleName());
	}
	
	public void connectRMI() {
		try {
			Registry clienteRMI = LocateRegistry.getRegistry(this.gatewayIp, this.gatewayPort);
			this.stubGateway = (IGatewayClientAction) clienteRMI.lookup("clientAction");
			try {
				String myIp = Inet4Address.getLocalHost().getHostAddress();
				String myHostName = Inet4Address.getLocalHost().getCanonicalHostName();
				String gatewayResp = this.stubGateway.helloGateway();
				if(!gatewayResp.isEmpty()) {
					log.info("Conectado al Gateway: " + this.gatewayIp + ":" + this.gatewayPort);
				}
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
			}
		} catch (RemoteException | NotBoundException e) {
			manageGatewayFall(this.gatewayIp, this.gatewayPort);
			connectRMI();
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
		connectRMI();
		if(this.file != null) {
			log.info("Enviando el archivo: "+this.file.getName());
			Trabajo work = new Trabajo(this.fileContent, file.getName(), startFrame, endFrame);
			try {
				byte[] zipReturnedBytes = this.stubGateway.renderRequest(work);
				if(zipReturnedBytes.length < 100) {
					return "Ha ocurrido un error. Por favor intentelo denuevo mas tarde";
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

			Map gateway = (Map) config.get("gateway");
			this.gatewayIp = gateway.get("ip").toString();
			this.gatewayPort = Integer.valueOf(gateway.get("port").toString());

			Map paths = (Map) config.get("paths");
			this.myRenderedImages = this.clienteDirectory + paths.get("renderedImages");

		} catch (IOException e) {
			log.error("Error Archivo Config!");
		} 
	}

}

