package blender.distributed.Cliente;

import blender.distributed.Enums.ENodo;
import blender.distributed.Records.RGateway;
import blender.distributed.Servidor.Cliente.IClienteAction;
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

    public static IClienteAction connectRandomGatewayRMI(List<RGateway> listaGateways, int tries, Logger log) throws RemoteException {
        MDC.put("log.name", ENodo.CLIENTE.name());
        tries--;
        if(tries <= 0) throw new RemoteException("Ningun gateway responde o no hay ninguno disponible.");
        IClienteAction stubGateway = null;
        if(listaGateways.size() > 0) {
            Random rand = new Random();
            int nRandomGateway = rand.nextInt(listaGateways.size());
            String ip = listaGateways.get(nRandomGateway).ip();
            int port = listaGateways.get(nRandomGateway).rmiPortForClientes();
            try {
                Registry clienteRMI = LocateRegistry.getRegistry(ip, port);
                stubGateway = (IClienteAction) clienteRMI.lookup("clienteAction");
                return stubGateway;
            } catch (RemoteException | NotBoundException e) {
                manageGatewayServidorFall(ENodo.GATEWAY,ip, port, log, ENodo.CLIENTE.name());
                return connectRandomGatewayRMI(listaGateways, tries, log);
            }
        } else {
            log.error("No hay ningun gateway disponible.");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            return connectRandomGatewayRMI(listaGateways, tries, log);
        }
    }
}
