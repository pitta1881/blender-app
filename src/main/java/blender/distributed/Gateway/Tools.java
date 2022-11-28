package blender.distributed.Gateway;

import blender.distributed.Enums.ENodo;
import blender.distributed.Gateway.Servidor.IGatewayServidorAction;
import blender.distributed.Records.RGateway;
import blender.distributed.Worker.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Random;

import static blender.distributed.SharedTools.Tools.manageGatewayServidorFall;

public class Tools {
    static Logger log = LoggerFactory.getLogger(Worker.class);

    public static IGatewayServidorAction connectRandomGatewayRMIForServidor(List<RGateway> listaGateways) {
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
                manageGatewayServidorFall(ENodo.GATEWAY, ip, port);
                return connectRandomGatewayRMIForServidor(listaGateways);
            }
        } else {
            log.error("No hay ningun gateway disponible.");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            return connectRandomGatewayRMIForServidor(listaGateways);
        }
    }
}