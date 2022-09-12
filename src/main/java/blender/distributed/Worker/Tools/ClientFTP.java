package blender.distributed.Worker.Tools;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.*;
import java.util.ArrayList;
  
public class ClientFTP {
	final String USER = "worker";
	final String PWD = "workerpwd";
	String ip;
	int port;
	FTPClient client;
	boolean logged = false;
	public ClientFTP(String ip, int port) throws java.net.ConnectException {
		try {
			this.client = new FTPClient();
			this.client.connect(ip,port);
			this.logged = client.login(USER, PWD);
			if (this.logged) {
				System.out.println("Conexion FTP Establecida.");
			} else {
			    System.out.println("Conexion FTP Fallida, revise si el usr/pwd es correcto: "+USER+"/"+PWD);
			}
		} catch (IOException e) {
			e.printStackTrace();
			try {
				this.client = new FTPClient();
				this.logged = client.login(USER, PWD);
				if (this.logged) {
					System.out.println("Conexion FTP Establecida.");
				} else {
				    System.out.println("Conexion FTP Fallida, revise si el usr/pwd es correcto: "+USER+"/"+PWD);
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	     
	public FTPClient getClient() {
		return this.client;
	}
	
	public ArrayList<String> showFiles() {
		ArrayList<String> result = new ArrayList<String>();
		if(this.logged) {
			try {
				FTPFile[] files = null;
				files = this.client.listFiles();
				for (FTPFile f : files) {
					result.add(f.getName());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	
	public void closeConn() {
		if(this.logged) {
			try {
				boolean logout = this.client.logout();
				if(logout) {
					this.logged = false;
					this.client.disconnect();
					System.out.println("Cerrando conexion FTP..");
				}else {
					System.err.println("Error al cerrar la conexion!");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	/*
	 *-----------------------------------------------
	 *|					FTP UTILS					|
	 *-----------------------------------------------
	 */
	public static boolean downloadSingleFile(FTPClient ftpClient,
	        String remoteFilePath, String savePath) throws IOException {

		File downloadFile = new File(savePath + remoteFilePath);
	    File parentDir = downloadFile.getParentFile();

	    if (!parentDir.exists()) {
	        parentDir.mkdir();
	    }
	    OutputStream outputStream = new BufferedOutputStream(
	            new FileOutputStream(downloadFile));
		System.out.println(outputStream);

		try {
	        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

			return ftpClient.retrieveFile(remoteFilePath, outputStream);
	    } catch (IOException ex) {
	        throw ex;
	    } finally {
	        if (outputStream != null) {
	            outputStream.close();
	        }
	    }
	}

}
