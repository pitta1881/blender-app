package blender.distributed.worker.SharedTools.Records;

public record RGateway(String uuid, String ip, int rmiPortForClientes, int rmiPortForWorkers, int rmiPortForServidores, long lastPing) {
}
