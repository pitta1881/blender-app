package blender.distributed.Servidor;

import java.io.*;
import java.nio.file.Files;


public class Mensaje implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String name;
	private byte[] blend;
	private byte[] zipWithRenderedImages;
	private int startFrame;
	private int endFrame;
	private String ipCliente;
	private int status = 1; //1: to do; 2:in progress; 3:done

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
	public void setZipWithRenderedImages(File zipRenderedImages) {
		try {
			zipWithRenderedImages = Files.readAllBytes(zipRenderedImages.toPath());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	public void setZipWithRenderedImages(byte[] zipRenderedImagesBytes) {
		zipWithRenderedImages = zipRenderedImagesBytes;
	}

	public byte[] getZipWithRenderedImages() {
		return this.zipWithRenderedImages;
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

	public Integer getStartFrame() {
		return startFrame;
	}
	public Integer getEndFrame() {
		return endFrame;
	}

	public int getStatus(){ return this.status;	}
	public void setStatus(int status){
		this.status = status;
	}

}
