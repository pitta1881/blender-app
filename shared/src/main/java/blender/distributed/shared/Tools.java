package blender.distributed.shared;

import blender.distributed.shared.Enums.ENodo;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class Tools {

    public static void manageGatewayServidorFall(ENodo nodo, String ip, int port, Logger log){
        log.error("Error al conectar con el " + nodo + " " + ip + ":" + port);
        try {
            log.info("Reintentando con el próximo " + nodo + " ...");
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
    }

    public static String getPublicIp(Logger log) {
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
