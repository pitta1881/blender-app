package blender.distributed.Records;

import blender.distributed.Enums.EStatus;

public record RParte(String uuidTrabajo, String uuid, int startFrame, int endFrame, EStatus estado, String urlZipFile) {
}
