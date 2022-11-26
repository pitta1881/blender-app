package blender.distributed.SharedTools;

import blender.distributed.Enums.ENodo;
import blender.distributed.Worker.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tools {
    static Logger log = LoggerFactory.getLogger(Worker.class);

    public static void manageGatewayServidorFall(ENodo nodo, String ip, int port){
        log.error("Error al conectar con el " + nodo + " " + ip + ":" + port);
        try {
            log.info("Reintentando con el próximo " + nodo + " ...");
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
