package blender.distributed.Servidor.helpers;


import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PasswordEncryptor;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("all")
public class ServerFtp {
	FtpServer server;
	
	public ServerFtp(int port, String directory) {
		FtpServerFactory serverFactory = new FtpServerFactory();
		ListenerFactory factory = new ListenerFactory();
		factory.setPort(port);
		serverFactory.addListener("default", factory.createListener());
		PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
		userManagerFactory.setFile(new File(directory+"\\..\\white.list"));//choose any. We're telling the FTP-server where to read it's user list
		userManagerFactory.setPasswordEncryptor(new PasswordEncryptor(){//We store clear-text passwords in this example
		        public String encrypt(String password) {
		            return password;
		        }
		        public boolean matches(String passwordToCheck, String storedPassword) {
		            return passwordToCheck.equals(storedPassword);
		        }
		    });
		    //Let's add a user, since our myusers.properties file is empty on our first test run
		    BaseUser user = new BaseUser();
		    user.setName("worker");
		    user.setPassword("workerpwd");
		    user.setHomeDirectory(directory);
		    List<Authority> authorities = new ArrayList<Authority>();
		    authorities.add(new WritePermission());
		    user.setAuthorities(authorities);
		    UserManager um = userManagerFactory.createUserManager();
		    try {
		        um.save(user);//Save the user to the user list on the filesystem
		    }catch (FtpException e1){
		        System.out.println("Error: "+e1.getMessage());
		    }
		    serverFactory.setUserManager(um);
		    Map<String, Ftplet> m = new HashMap<String, Ftplet>();
		    m.put("miaFtplet", new Ftplet(){
		        public void init(FtpletContext ftpletContext) throws FtpException {
		            System.out.println("init");
		            System.out.println("Thread #" + Thread.currentThread().getId());
		        }
		        public void destroy() {
		            System.out.println("destroy");
		            System.out.println("Thread #" + Thread.currentThread().getId());
		        }
		        public FtpletResult beforeCommand(FtpSession session, FtpRequest request) throws FtpException, IOException{
		            System.out.println("beforeCommand " + session.getUserArgument() + " : " + session.toString() + " | " + request.getArgument() + " : " + request.getCommand() + " : " + request.getRequestLine());
		            System.out.println("Thread #" + Thread.currentThread().getId());

		            //do something
		            return FtpletResult.DEFAULT;//...or return accordingly
		        }
		        public FtpletResult afterCommand(FtpSession session, FtpRequest request, FtpReply reply) throws FtpException, IOException{
		            System.out.println("afterCommand " + session.getUserArgument() + " : " + session.toString() + " | " + request.getArgument() + " : " + request.getCommand() + " : " + request.getRequestLine() + " | " + reply.getMessage() + " : " + reply.toString());
		            System.out.println("Thread #" + Thread.currentThread().getId());

		            //do something
		            return FtpletResult.DEFAULT;//...or return accordingly
		        }
		        public FtpletResult onConnect(FtpSession session) throws FtpException, IOException{
		            System.out.println("onConnect " + session.getUserArgument() + " : " + session.toString());
		            System.out.println("Thread #" + Thread.currentThread().getId());

		            //do something
		            return FtpletResult.DEFAULT;//...or return accordingly
		        }
		        public FtpletResult onDisconnect(FtpSession session) throws FtpException, IOException{
		            System.out.println("onDisconnect " + session.getUserArgument() + " : " + session.toString());
		            System.out.println("Thread #" + Thread.currentThread().getId());
		            System.out.println();

		            //do something
		            return FtpletResult.DEFAULT;//...or return accordingly
		        }
		    });
		    serverFactory.setFtplets(m);
		    Map<String, Ftplet> mappa = serverFactory.getFtplets();
		    System.out.println(mappa.size());
		    System.out.println("Thread #" + Thread.currentThread().getId());
		    System.out.println(mappa.toString());
		    this.server = serverFactory.createServer();
	}
	public boolean startServer() {
		try {
	        server.start();
	        return true;
	    }catch (FtpException ex){
	        System.out.println("Error: "+ex.getMessage());
	        return false;
	    }
	}
	public void stopServer() {
		this.server.suspend();
	}

	public void resumeServer() {
		this.server.resume();
	}
	
	public FtpServer getServer() {
		return this.server;
	}
}