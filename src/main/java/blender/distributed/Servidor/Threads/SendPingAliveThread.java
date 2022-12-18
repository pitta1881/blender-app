package blender.distributed.Servidor.Threads;

import blender.distributed.Enums.EServicio;
import blender.distributed.Gateway.Servidor.IServidorAction;
import blender.distributed.Records.RGateway;
import blender.distributed.SharedTools.Tools;
import org.slf4j.Logger;

import java.rmi.RemoteException;
import java.util.List;

public class SendPingAliveThread implements Runnable {
    Logger log;
    List<RGateway> listaGateways;
    String uuid;
    String myPublicIp;
    int rmiPort;
    int rmiPortForWorkers;
    public SendPingAliveThread(String uuid, String myPublicIp, List<RGateway> listaGateways, int rmiPort, Logger log) {
        this.uuid = uuid;
        this.myPublicIp = myPublicIp;
        this.listaGateways = listaGateways;
        this.rmiPort = rmiPort;
        this.log = log;
    }

    @Override
    public void run() {
        while(true) {
            try {
                Thread.sleep(5000);
                Tools.<IServidorAction>connectRandomGatewayRMI(this.listaGateways, EServicio.SERVIDOR_ACTION, -1, this.log).pingAliveFromServidor(this.uuid, this.myPublicIp, this.rmiPort);
            } catch (InterruptedException | RemoteException e) {
                log.error("Error: " + e.getMessage());;
            }
        }
    }
}
