package blender.distributed.Records;

import blender.distributed.Enums.EStatus;

import java.util.List;

public record RTrabajo(String uuid, String blendName, int startFrame, int endFrame, EStatus estado, List<String> listaPartes, String gStorageBlendName, String gStorageZipName, String createdAt, String finishedAt) {
}
