package blender.distributed.servidor.Threads;

import blender.distributed.shared.Records.RGateway;
import org.slf4j.Logger;


import java.rmi.RemoteException;
import java.util.List;

import static blender.distributed.servidor.Tools.connectRandomGatewayRMIForServidor;


public class SendPingAliveThread implements Runnable {
    Logger log;
    List<RGateway> listaGateways;
    String uuid;
    String myPublicIp;
    int rmiPortForClientes;
    int rmiPortForWorkers;
    public SendPingAliveThread(String uuid, String myPublicIp, List<RGateway> listaGateways, int rmiPortForClientes, int rmiPortForWorkers, Logger log) {
        this.uuid = uuid;
        this.myPublicIp = myPublicIp;
        this.listaGateways = listaGateways;
        this.rmiPortForClientes = rmiPortForClientes;
        this.rmiPortForWorkers = rmiPortForWorkers;
        this.log = log;
    }

    @Override
    public void run() {
        while(true) {
            try {
                Thread.sleep(5000);
                connectRandomGatewayRMIForServidor(this.listaGateways, this.log).pingAliveFromServidor(this.uuid, this.myPublicIp, this.rmiPortForClientes, this.rmiPortForWorkers);
            } catch (InterruptedException | RemoteException e) {
                log.error("Error: " + e.getMessage());;
            }
        }
    }
}
