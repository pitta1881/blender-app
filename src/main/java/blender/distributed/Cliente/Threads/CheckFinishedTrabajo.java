package blender.distributed.Cliente.Threads;

import blender.distributed.Enums.EStatus;
import blender.distributed.Records.RGateway;
import blender.distributed.Records.RTrabajo;
import blender.distributed.Servidor.Cliente.ClienteAction;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.rmi.RemoteException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

import static blender.distributed.Cliente.Tools.connectRandomGatewayRMI;

public class CheckFinishedTrabajo implements Runnable{
    Logger log = LoggerFactory.getLogger(ClienteAction.class);
    List<RGateway> listaGateways;
    RTrabajo recordTrabajo;
    Gson gson = new Gson();
    Type RTrabajoType = new TypeToken<RTrabajo>(){}.getType();


    public CheckFinishedTrabajo(List<RGateway> listaGateways, RTrabajo recordTrabajo){
        this.listaGateways = listaGateways;
        this.recordTrabajo = recordTrabajo;
    }

    @Override
    public void run() {
        boolean salir = false;
        LocalTime initTime = LocalTime.now();
        log.info("Trabajo enviado: " + this.recordTrabajo.toString());
        log.info("Tiempo inicio:\t" + initTime.toString());
        String recordTrabajoJson;
        RTrabajo recordTrabajo;
        while (!salir) {
            try {
                recordTrabajoJson = connectRandomGatewayRMI(this.listaGateways).getTrabajo(this.recordTrabajo.uuid());
                recordTrabajo = gson.fromJson(recordTrabajoJson, RTrabajoType);
                if (recordTrabajo != null && recordTrabajo.estado() == EStatus.DONE) {
                    salir = true;
                }
                Thread.sleep(1000);
            } catch (RemoteException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        LocalTime finishTime = LocalTime.now();
        log.info("Tiempo fin:\t" + finishTime.toString());
        log.info("Tiempo tardado:\t\t" + Duration.between(initTime, finishTime).toSeconds() + " segundos.");
        //TODO: get zip/download link from somewhere
    }
}
