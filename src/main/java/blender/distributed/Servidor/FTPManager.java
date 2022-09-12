package blender.distributed.Servidor;

import blender.distributed.Servidor.helpers.ServerFtp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;

public class FTPManager implements IFTPManager{
	Logger log = LoggerFactory.getLogger(FTPManager.class);
	//Ftp Related
	private int ftpPort;
	ServerFtp ftp;
	
	public FTPManager(int ftpPort, ServerFtp ftp) {
		this.ftp = ftp;
		this.ftpPort = ftpPort;
	}
	
	@Override
	public int startFTPServer() throws RemoteException {
		try {
			if(this.ftp.startServer()) {
				log.info("Iniciando servidor FTP...");
				return this.ftpPort;
			}else {
				return 0;
			}
		}catch (Exception e) {
			log.debug(e.getMessage());
			log.debug("Intentaron levantar el servidor FTP pero ya estaba instanciado");
			return -1;
		}
		
	}
	@Override
	public boolean stopFTPServer() throws RemoteException {
		log.info("Parando servidor FTP...");
		if(this.ftp != null) {
			this.ftp.stopServer();
			return true;
		}else {
			return false;
		}
	}
	@Override
	public int getFTPPort() throws RemoteException {
		return this.ftpPort;
	}
	@Override
	public boolean isFTPStopped() throws RemoteException {
		return this.ftp.getServer().isStopped();
	}
	@Override
	public boolean resumeFTPServer() throws RemoteException {
		this.ftp.resumeServer();
		return !this.ftp.getServer().isStopped();
	}

}
