package blender.distributed.Records;

public record RGateway(String uuid, String ip, int rmiPortForClientes, int rmiPortForWorkers, int rmiPortForServidores) {
}
