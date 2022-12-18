package blender.distributed.Records;

public record RServidor(String uuid, String ip, int rmiPort, long lastPing) {
}
