package blender.distributed.Cliente.Threads;

import blender.distributed.Cliente.view.GUIFinishedWork;
import blender.distributed.Enums.EServicio;
import blender.distributed.Enums.EStatus;
import blender.distributed.Records.RGateway;
import blender.distributed.Records.RTrabajo;
import blender.distributed.Servidor.Cliente.IClienteAction;
import blender.distributed.SharedTools.Tools;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;

import java.lang.reflect.Type;
import java.rmi.RemoteException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.IntStream;

public class CheckFinishedTrabajo implements Runnable{
    Logger log;
    List<RGateway> listaGateways;
    List<RTrabajo> listaTrabajos;
    RTrabajo recordTrabajo;
    Gson gson = new Gson();
    Type RTrabajoType = new TypeToken<RTrabajo>(){}.getType();

    int tries;

    public CheckFinishedTrabajo(List<RGateway> listaGateways, List<RTrabajo> listaTrabajos, RTrabajo recordTrabajo, int tries, Logger log){
        this.listaGateways = listaGateways;
        this.listaTrabajos = listaTrabajos;
        this.recordTrabajo = recordTrabajo;
        this.tries = tries;
        this.log = log;
    }

    @Override
    public void run() {
        boolean salir = false;
        LocalTime initTime = LocalTime.now();
        log.info("Trabajo enviado: " + this.recordTrabajo.toString());
        log.info("Tiempo inicio:\t" + initTime.toString());
        String recordTrabajoJson;
        RTrabajo recordTrabajo = null;
        while (!salir) {
            try {
                recordTrabajoJson = Tools.<IClienteAction>connectRandomGatewayRMI(this.listaGateways, EServicio.CLIENTE_ACTION, this.tries, this.log).getTrabajo(this.recordTrabajo.uuid());
                recordTrabajo = gson.fromJson(recordTrabajoJson, RTrabajoType);
                synchronized (this.listaTrabajos) {
                    RTrabajo finalRecordTrabajo = recordTrabajo;
                    int i = IntStream.range(0, this.listaTrabajos.size()).filter(index -> this.listaTrabajos.get(index).uuid().equals(finalRecordTrabajo.uuid())).findFirst().getAsInt();
                    this.listaTrabajos.remove(i);
                    this.listaTrabajos.add(finalRecordTrabajo);
                }
                if (recordTrabajo != null && recordTrabajo.estado() == EStatus.DONE && recordTrabajo.gStorageZipName() != null) {
                    salir = true;
                }
                Thread.sleep(1000);
            } catch (RemoteException | InterruptedException e) {
                log.error("Error: " + e.getMessage());
            }
        }
        LocalTime finishTime = LocalTime.now();
        Long timeTaken = Duration.between(initTime, finishTime).toSeconds();
        log.info("Tiempo fin:\t" + finishTime.toString());
        log.info("Tiempo tardado:\t" + timeTaken + " segundos.");
        new GUIFinishedWork(recordTrabajo.blendName(), recordTrabajo.gStorageZipName(), timeTaken).setVisible(true);
    }

}
