package blender.distributed.shared.Records;

public record RServidor(String uuid, String ip, int rmiPortForClientes, int rmiPortForWorkers, long lastPing) {
}
