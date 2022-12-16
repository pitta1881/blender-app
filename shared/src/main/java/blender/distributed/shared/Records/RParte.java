package blender.distributed.worker.SharedTools.Records;

import blender.distributed.worker.SharedTools.Enums.EStatus;

public record RParte(String uuidTrabajo, String uuid, int startFrame, int endFrame, EStatus estado, String gStorageZipName) {
}
