package blender.distributed.Gateway;

import blender.distributed.Enums.ENodo;
import blender.distributed.Enums.EServicio;
import blender.distributed.Records.RGateway;
import blender.distributed.Records.RServidor;
import blender.distributed.Servidor.Cliente.IClienteAction;
import org.slf4j.Logger;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Random;

import static blender.distributed.SharedTools.Tools.manageGatewayServidorFall;

public class Tools {
    public static <T> T connectRandomServidorRMI(List<RServidor> listaServidores, EServicio servicio, Logger log) throws RemoteException {
        T stubServidor = null;
        if(listaServidores.size() > 0) {
            Random rand = new Random();
            int nRandomServidor = rand.nextInt(listaServidores.size());
            String ip = listaServidores.get(nRandomServidor).ip();
            int port = listaServidores.get(nRandomServidor).rmiPort();
            try {
                Registry clienteRMI = LocateRegistry.getRegistry(ip, port);
                stubServidor = (T) clienteRMI.lookup(servicio.name());
                return stubServidor;
            } catch (RemoteException | NotBoundException e) {
                log.error("Error: " + e.getMessage());
                manageGatewayServidorFall(ENodo.SERVIDOR, ip, port, log);
                return connectRandomServidorRMI(listaServidores, servicio, log);
            }
        } else {
            log.error("No hay ningun servidor disponible.");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            return connectRandomServidorRMI(listaServidores, servicio, log);
        }
    }
}
