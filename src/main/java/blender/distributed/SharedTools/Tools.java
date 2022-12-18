package blender.distributed.SharedTools;

import blender.distributed.Enums.ENodo;
import blender.distributed.Enums.EServicio;
import blender.distributed.Records.RGateway;
import org.slf4j.Logger;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Random;

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

    public static <T> T connectRandomGatewayRMI(List<RGateway> listaGateways, EServicio servicio, int tries, Logger log) throws RemoteException {
        if(tries != -1) {
            tries--;
            if (tries <= 0) throw new RemoteException("Ningun gateway responde o no hay ninguno disponible.");
        }
        T stubGateway = null;
        if(listaGateways.size() > 0) {
            Random rand = new Random();
            int nRandomGateway = rand.nextInt(listaGateways.size());
            String ip = listaGateways.get(nRandomGateway).ip();
            int port = listaGateways.get(nRandomGateway).rmiPort();
            try {
                Registry workerRMI = LocateRegistry.getRegistry(ip, port);
                stubGateway = (T) workerRMI.lookup(servicio.name());
                return stubGateway;
            } catch (RemoteException | NotBoundException e) {
                manageGatewayServidorFall(ENodo.GATEWAY, ip, port, log);
                return connectRandomGatewayRMI(listaGateways, servicio, tries, log);
            }
        } else {
            log.error("No hay ningun gateway disponible.");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            return connectRandomGatewayRMI(listaGateways, servicio, tries, log);
        }
    }
}
