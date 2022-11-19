package blender.distributed.Gateway;

import blender.distributed.Servidor.Trabajo.PairTrabajoParte;

import java.io.Serializable;
import java.time.LocalTime;

public record PairParteLastping(PairTrabajoParte ptp, LocalTime lastPing) implements Serializable {
}
