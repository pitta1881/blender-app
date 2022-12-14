package blender.distributed.Worker;

import blender.distributed.Enums.ENodo;
import blender.distributed.Records.RGateway;
import blender.distributed.Servidor.Worker.IWorkerAction;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Random;

import static blender.distributed.SharedTools.Tools.manageGatewayServidorFall;

public class Tools {

    public static IWorkerAction connectRandomGatewayRMI(List<RGateway> listaGateways, Logger log) {
        MDC.put("log.name", ENodo.WORKER.name());
        IWorkerAction stubGateway = null;
        if(listaGateways.size() > 0) {
            Random rand = new Random();
            int nRandomGateway = rand.nextInt(listaGateways.size());
            String ip = listaGateways.get(nRandomGateway).ip();
            int port = listaGateways.get(nRandomGateway).rmiPortForWorkers();
            try {
                Registry workerRMI = LocateRegistry.getRegistry(ip, port);
                stubGateway = (IWorkerAction) workerRMI.lookup("workerAction");
                return stubGateway;
            } catch (RemoteException | NotBoundException e) {
                manageGatewayServidorFall(ENodo.GATEWAY,ip, port, log, ENodo.WORKER.name());
                return connectRandomGatewayRMI(listaGateways, log);
            }
        } else {
            log.error("No hay ningun gateway disponible.");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            return connectRandomGatewayRMI(listaGateways, log);
        }
    }
}
