package blender.distributed.shared.Records;


import blender.distributed.shared.Enums.EStatus;

public record RParte(String uuidTrabajo, String uuid, int startFrame, int endFrame, EStatus estado, String gStorageZipName) {
}
