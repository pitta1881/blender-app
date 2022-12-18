package blender.distributed.Records;

public record RGateway(String uuid, String ip, int rmiPort, long lastPing) {
}
