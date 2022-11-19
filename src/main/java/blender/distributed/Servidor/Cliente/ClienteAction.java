package blender.distributed.Servidor.Cliente;

import blender.distributed.Gateway.Servidor.IGatewayServidorAction;
import blender.distributed.Servidor.Trabajo.Trabajo;
import blender.distributed.Servidor.Trabajo.TrabajoStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.Duration;
import java.time.LocalTime;

public class ClienteAction implements IClientAction {
	Logger log = LoggerFactory.getLogger(ClienteAction.class);
	String gatewayIp;
	int gatewayPort;


	public ClienteAction(String gatewayIp, int gatewayPort) {
		MDC.put("log.name", ClienteAction.class.getSimpleName());
		this.gatewayIp = gatewayIp;
		this.gatewayPort = gatewayPort;
	}

	@Override
	public byte[] renderRequest(Trabajo work) {
		try {
			this.connectRMI().setTrabajo(work);

			boolean salir = false;
			LocalTime initTime = LocalTime.now();
			log.info("Trabajo iniciado: " + work.getId());
			log.info("Trabajando en: " + work.getBlendName() + " - Frames " + work.getStartFrame() + "-" + work.getEndFrame());
			log.info("Tiempo inicio:\t" + initTime.toString());
			Trabajo workFinished = work;
			String workId = work.getId();
			while (!salir) {
				try {
					workFinished = this.connectRMI().getTrabajo(workId);
					if (workFinished != null && workFinished.getStatus() == TrabajoStatus.DONE && workFinished.getZipWithRenderedImages() != null) {
						salir = true;
					}

					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
			LocalTime finishTime = LocalTime.now();
			log.info("Tiempo fin:\t" + finishTime.toString());
			log.info("Tiempo tardado:\t\t" + Duration.between(initTime, finishTime).toSeconds() + " segundos.");
			log.info("Trabajo completado: " + workFinished.getId());

			this.connectRMI().delTrabajo(workFinished.getId());
			return workFinished.getZipWithRenderedImages();
		} catch (RemoteException | NullPointerException e) {
			return renderRequest(work);
		}
	}

	private IGatewayServidorAction connectRMI() {
		IGatewayServidorAction stubGateway = null;
		try {
			Thread.sleep(1000);
			Registry workerRMI = LocateRegistry.getRegistry(this.gatewayIp, this.gatewayPort);
			stubGateway = (IGatewayServidorAction) workerRMI.lookup("servidorAction");
			return stubGateway;
		} catch (RemoteException | NotBoundException | InterruptedException e) {
			log.error("Error al conectar con el Gateway " + this.gatewayIp + ":" + this.gatewayPort);
			return connectRMI();
		}
	}
}
