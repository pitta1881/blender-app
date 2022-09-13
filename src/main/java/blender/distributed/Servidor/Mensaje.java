package blender.distributed.Servidor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;


public class Mensaje implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String name;
	byte[] blend;
	byte[] bufferedImg;
	int startFrame;
	int endFrame;
	String from;
	String ipCliente;
	int nroRender;

	//Mensaje simple solo string de Servidor -> worker
	public Mensaje(String name) {
		this.name = name;
	}
	
	
	//Mensaje enviado por el cliente
	public Mensaje(byte[] blend, String name, int startFrame, int endFrame, String ipCliente){
		this.blend = blend;
		this.startFrame = startFrame;
		this.endFrame = endFrame;
		this.name = name;
		this.ipCliente = ipCliente;
	}
	
	//Mensaje armado por el worker
	public Mensaje(BufferedImage bufferedImg, String name, String from, String ipCliente, int nroRender){
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			ImageIO.write(bufferedImg, "png", outputStream);
			this.bufferedImg = outputStream.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.name = name;
		this.from = from;
		this.ipCliente = ipCliente;
		this.nroRender = nroRender;
	}
	
	
	public byte[] getBytes(){
		try {
        	ByteArrayOutputStream bs= new ByteArrayOutputStream();
        	ObjectOutputStream os = new ObjectOutputStream (bs);
			os.writeObject(this);
	    	os.close();
	    	return  bs.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}  
	}
	
	public static Mensaje getMensaje(byte[] bData) {
		try {
			ByteArrayInputStream bs = new ByteArrayInputStream(bData); 
			ObjectInputStream is = new ObjectInputStream(bs);
			Mensaje msg = (Mensaje)is.readObject();
			is.close();
			return msg;
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public byte[] getBlend() {
		return blend;
	}

	public void setBlend(byte[] blend) {
		this.blend = blend;
	}

	public String getIpCliente() {
		return ipCliente;
	}

	public void setIpCliente(String ipCliente) {
		this.ipCliente = ipCliente;
	}
}
