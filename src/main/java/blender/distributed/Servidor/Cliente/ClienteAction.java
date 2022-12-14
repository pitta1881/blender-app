package blender.distributed.Servidor.Cliente;

import blender.distributed.Enums.ENodo;
import blender.distributed.Enums.EStatus;
import blender.distributed.Records.RGateway;
import blender.distributed.Records.RParte;
import blender.distributed.Records.RTrabajo;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static blender.distributed.Servidor.Tools.connectRandomGatewayRMIForServidor;

public class ClienteAction implements IClienteAction {
	Logger log;
	List<RGateway> listaGateways;
	int frameDivision;
	Gson gson = new Gson();

	public ClienteAction(List<RGateway> listaGateways, int frameDivision, Logger log) {
		MDC.put("log.name", ENodo.SERVIDOR.name());
		this.listaGateways = listaGateways;
		this.frameDivision = frameDivision;
		this.log = log;
	}
    @Override
	public String renderRequest(byte[] blendFile, String blendName, int startFrame, int endFrame) {
		String uuid = UUID.randomUUID().toString();
		try {
			connectRandomGatewayRMIForServidor(this.listaGateways, this.log).storeBlendFile(uuid+".blend", blendFile);
			List<String> listaUUIDPartes = dividirEnPartes(uuid, startFrame, endFrame);
			RTrabajo recordTrabajo = new RTrabajo(uuid, blendName, startFrame, endFrame, EStatus.TO_DO, listaUUIDPartes, uuid+".blend", null, LocalDateTime.now().toString());
			String json = gson.toJson(recordTrabajo);
			connectRandomGatewayRMIForServidor(this.listaGateways, this.log).setTrabajo(uuid, json);
			log.info("Registrando nuevo trabajo: " + recordTrabajo);
			return json;
		} catch (RemoteException | NullPointerException e) {
			return renderRequest(blendFile, blendName, startFrame, endFrame);
		}
	}

	@Override
	public String getTrabajo(String uuid) {
		try {
			return connectRandomGatewayRMIForServidor(this.listaGateways, this.log).getTrabajo(uuid);
		} catch (RemoteException e) {
			return getTrabajo(uuid);
		}
	}

	private List<String> dividirEnPartes(String uuidTrabajo, int startFrame, int endFrame) {
		List<String> uuidPartes = new ArrayList<>();
		int totalFrames = endFrame - startFrame + 1;
		int cantidadPartes = (int) Math.ceil((float)totalFrames / (float)this.frameDivision);
		int partStartFrame = startFrame;
		int partEndFrame = this.frameDivision;
		if (cantidadPartes == 1) {
			partEndFrame = endFrame;
		}
		for (int i = 0; i < cantidadPartes; i++) {
			String uuid = UUID.randomUUID().toString();
			uuidPartes.add(uuid);
			RParte recordParte = new RParte(uuidTrabajo, uuid, partStartFrame, partEndFrame, EStatus.TO_DO, null);
			try {
				connectRandomGatewayRMIForServidor(this.listaGateways, this.log).setParte(uuid, gson.toJson(recordParte));
			} catch (RemoteException e) {
				log.error("Error: " + e.getMessage());
			}
			partStartFrame = partEndFrame + 1;
			partEndFrame += this.frameDivision;
			if (partEndFrame > endFrame) {
				partEndFrame = endFrame;
			}
		}
		return uuidPartes;
	}
}
