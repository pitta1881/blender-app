package blender.distributed.worker.SharedTools.Records;

import blender.distributed.worker.SharedTools.Enums.EStatus;

import java.util.List;

public record RTrabajo(String uuid, String blendName, int startFrame, int endFrame, EStatus estado, List<String> listaPartes, String gStorageBlendName, String gStorageZipName, String createdAt, String finishedAt) {
}
