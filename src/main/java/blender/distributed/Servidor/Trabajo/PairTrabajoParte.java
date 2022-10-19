package blender.distributed.Servidor.Trabajo;

import java.io.Serializable;

public record PairTrabajoParte(Trabajo trabajo, TrabajoPart parte) implements Serializable {

}
