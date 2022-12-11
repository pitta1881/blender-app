package blender.distributed.SharedTools;

import blender.distributed.Enums.ENodo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class Tools {
    static Logger log = LoggerFactory.getLogger(Tools.class);

    public static void manageGatewayServidorFall(ENodo nodo, String ip, int port){
        log.error("Error al conectar con el " + nodo + " " + ip + ":" + port);
        try {
            log.info("Reintentando con el próximo " + nodo + " ...");
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
    }

    public static String getPublicIp() {
        URL whatismyip = null;
        String ip = null;
        try {
            whatismyip = new URL("http://checkip.amazonaws.com");
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()));
            ip = in.readLine();
        } catch (IOException e) {
            log.error("Error: " + e.getMessage());
        }
        return ip;
    }
}
