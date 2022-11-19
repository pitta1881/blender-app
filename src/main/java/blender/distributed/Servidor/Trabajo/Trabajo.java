package blender.distributed.Servidor.Trabajo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;


public class Trabajo implements Serializable {
	String id;
	private String blendName;
	private byte[] blendFile;
	private int startFrame;
	private int endFrame;
	private byte[] zipWithRenderedImages = null;
	private final int frameDivision = 100;
	private ArrayList<TrabajoPart> listaPartes;


	//Mensaje enviado por el cliente
	public Trabajo(byte[] blendFile, String blendName, int startFrame, int endFrame){
		this.id = UUID.randomUUID().toString();
		this.blendFile = blendFile;
		this.blendName = blendName;
		this.startFrame = startFrame;
		this.endFrame = endFrame;
		this.listaPartes = this.dividirEnPartes();
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
	public TrabajoStatus getStatus(){
		boolean inToDo = this.listaPartes.stream().anyMatch(parte -> parte.getStatus() == TrabajoStatus.TO_DO);
		if(inToDo){
			return TrabajoStatus.TO_DO;
		}
		boolean inProgress = this.listaPartes.stream().anyMatch(parte -> parte.getStatus() == TrabajoStatus.IN_PROGRESS);
		if(inProgress){
			return TrabajoStatus.IN_PROGRESS;
		}
		return TrabajoStatus.DONE;
	}

	public byte[] getZipWithRenderedImages(){ return this.zipWithRenderedImages; }
	public void setZipWithRenderedImages(byte[] zipWithRenderedImages){
		this.zipWithRenderedImages = zipWithRenderedImages;
	}
	public ArrayList<TrabajoPart> getListaPartes() {
		return this.listaPartes;
	}
	public TrabajoPart getParte(int nParte) {
		TrabajoPart parteFound = listaPartes.stream().filter(parte -> nParte == parte.getNParte()).findFirst().orElse(null);
		return parteFound;
	}
	private ArrayList<TrabajoPart> dividirEnPartes() {
		ArrayList<TrabajoPart> listPart = new ArrayList<>();
		int totalFrames = endFrame - startFrame + 1;
		int cantidadPartes = (int) Math.ceil((float)totalFrames / (float)frameDivision);
		int partStartFrame = startFrame;
		int partEndFrame = frameDivision;
		if (cantidadPartes == 1) {
			partEndFrame = endFrame;
		}
		for (int i = 0; i < cantidadPartes; i++) {
			TrabajoPart part = new TrabajoPart(i+1, partStartFrame, partEndFrame);
			listPart.add(part);
			partStartFrame = partEndFrame + 1;
			partEndFrame += frameDivision;
			if (partEndFrame > endFrame) {
				partEndFrame = endFrame;
			}
		}
		return listPart;
	}
}

