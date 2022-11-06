package blender.distributed.SharedTools;

import blender.distributed.Worker.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tools {
    static Logger log = LoggerFactory.getLogger(Worker.class);

    public static void manageGatewayFall(String gatewayIp, int gatewayPort){
        log.error("Error al conectar con el Gateway " + gatewayIp + ":" + gatewayPort);
        try {
            log.info("Reintentando conectar...");
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
