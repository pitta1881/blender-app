package blender.distributed.Servidor.Trabajo;

import java.io.Serializable;
import java.util.UUID;


public class Trabajo implements Serializable{

	String id;
	private String blendName;
	private byte[] blendFile;
	private int startFrame;
	private int endFrame;
	private TrabajoStatus status = TrabajoStatus.TO_DO;
	byte[] zipWithRenderedImages = null;


	//Mensaje enviado por el cliente
	public Trabajo(byte[] blendFile, String blendName, int startFrame, int endFrame){
		this.id = UUID.randomUUID().toString();
		this.blendFile = blendFile;
		this.blendName = blendName;
		this.startFrame = startFrame;
		this.endFrame = endFrame;
	}

	public String getId() {
		return this.id;
	}
	public String getBlendName() {
		return this.blendName;
	}
	public byte[] getBlendFile() {
		return this.blendFile;
	}
	public Integer getStartFrame() {
		return startFrame;
	}
	public Integer getEndFrame() {
		return endFrame;
	}
	public TrabajoStatus getStatus(){ return this.status;	}
	public void setStatus(TrabajoStatus status){
		this.status = status;
	}
	public byte[] getZipWithRenderedImages(){ return this.zipWithRenderedImages; }
	public void setZipWithRenderedImages(byte[] zipWithRenderedImages){ this.zipWithRenderedImages = zipWithRenderedImages; }


}
