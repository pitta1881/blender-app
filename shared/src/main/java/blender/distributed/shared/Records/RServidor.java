package blender.distributed.worker.SharedTools.Records;

public record RServidor(String uuid, String ip, int rmiPortForClientes, int rmiPortForWorkers, long lastPing) {
}
