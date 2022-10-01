package blender.distributed.Servidor.Trabajo;

import java.io.Serializable;

public class TrabajoPart implements Serializable {
    private int nParte;
    private int startFrame;
    private int endFrame;
    private TrabajoStatus status = TrabajoStatus.TO_DO;
    private byte[] zipWithRenderedImages = null;

    public TrabajoPart(int partNumber, int startFrame, int endFrame){
        this.nParte = partNumber;
        this.startFrame = startFrame;
        this.endFrame = endFrame;
    }

    public int getNParte() { return nParte; }

    public int getStartFrame() { return startFrame; }

    public int getEndFrame() { return endFrame; }

    public TrabajoStatus getStatus() { return status; }

    public void setStatus(TrabajoStatus status) { this.status = status; }

    public byte[] getZipWithRenderedImages() { return zipWithRenderedImages; }

    public void setZipWithRenderedImages(byte[] zipWithRenderedImages) { this.zipWithRenderedImages = zipWithRenderedImages; }

    @Override
    public String toString() {
        return   "Parte Nº: " + nParte + "\n"
                +"Start Frame: " + startFrame + "\n"
                +"End Frame: " + endFrame + "\n"
                +"Estado: " + status;
    }
}
