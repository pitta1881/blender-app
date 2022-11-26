package blender.distributed.Servidor.Cliente;

import blender.distributed.Enums.EStatus;
import blender.distributed.Records.RGateway;
import blender.distributed.Records.RParte;
import blender.distributed.Records.RTrabajo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.Type;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static blender.distributed.Gateway.Tools.connectRandomGatewayRMIForServidor;

public class ClienteAction implements IClienteAction {
	Logger log = LoggerFactory.getLogger(ClienteAction.class);
	List<RGateway> listaGateways;
	int frameDivision = 100;
	Gson gson = new Gson();
	Type RTrabajoType = new TypeToken<RTrabajo>(){}.getType();

	public ClienteAction(List<RGateway> listaGateways) {
		MDC.put("log.name", ClienteAction.class.getSimpleName());
		this.listaGateways = listaGateways;
	}
    @Override
	public String renderRequest(byte[] blendFile, String blendName, int startFrame, int endFrame) {
		String uuid = UUID.randomUUID().toString();
		try {
			String url = connectRandomGatewayRMIForServidor(this.listaGateways).storeBlendFile(blendName, blendFile);
			List<String> listaUUIDPartes = dividirEnPartes(uuid, startFrame, endFrame);
			RTrabajo recordTrabajo = new RTrabajo(uuid, blendName, startFrame, endFrame, EStatus.TO_DO, listaUUIDPartes, url);
			String json = gson.toJson(recordTrabajo);
			connectRandomGatewayRMIForServidor(this.listaGateways).setTrabajo(uuid, json);
			log.info("Registrando nuevo trabajo: " + recordTrabajo);
			return json;
		} catch (RemoteException | NullPointerException e) {
			e.printStackTrace();
			return renderRequest(blendFile, blendName, startFrame, endFrame);
		}
	}

	@Override
	public String getTrabajo(String uuid) {
		try {
			return connectRandomGatewayRMIForServidor(this.listaGateways).getTrabajo(uuid);
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
				connectRandomGatewayRMIForServidor(this.listaGateways).setParte(uuid, gson.toJson(recordParte));
			} catch (RemoteException e) {
				throw new RuntimeException(e);
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
