package blender.distributed.servidor;

import blender.distributed.Enums.ENodo;
import blender.distributed.gateway.Servidor.IGatewayServidorAction;
import blender.distributed.Records.RGateway;
import org.slf4j.Logger;


import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Random;

import static blender.distributed.SharedTools.Tools.manageGatewayServidorFall;

public class Tools {

    public static IGatewayServidorAction connectRandomGatewayRMIForServidor(List<RGateway> listaGateways, Logger log) {
        IGatewayServidorAction stubGateway = null;
        if(listaGateways.size() > 0) {
            Random rand = new Random();
            int nRandomGateway = rand.nextInt(listaGateways.size());
            String ip = listaGateways.get(nRandomGateway).ip();
            int port = listaGateways.get(nRandomGateway).rmiPortForServidores();
            try {
                Registry servidorRMI = LocateRegistry.getRegistry(ip, port);
                stubGateway = (IGatewayServidorAction) servidorRMI.lookup("servidorAction");
                return stubGateway;
            } catch (RemoteException | NotBoundException e) {
                log.error("Error: " + e.getMessage());
                manageGatewayServidorFall(ENodo.GATEWAY, ip, port, log);
                return connectRandomGatewayRMIForServidor(listaGateways, log);
            }
        } else {
            log.error("No hay ningun gateway disponible.");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            return connectRandomGatewayRMIForServidor(listaGateways, log);
        }
    }
}
