package blender.distributed.Gateway;

import java.io.Serializable;

public record PairIpPortCPortW(String ip, int rmiPortForClientes, int rmiPortForWorkers) implements Serializable {
}
