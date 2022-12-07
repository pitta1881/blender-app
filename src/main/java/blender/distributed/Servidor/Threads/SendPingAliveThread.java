package blender.distributed.Servidor.Threads;

import blender.distributed.Records.RGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.List;

import static blender.distributed.Gateway.Tools.connectRandomGatewayRMIForServidor;

public class SendPingAliveThread implements Runnable {
    static Logger log = LoggerFactory.getLogger(SendPingAliveThread.class);
    List<RGateway> listaGateways;
    String uuid;
    String myPublicIp;
    int rmiPortForClientes;
    int rmiPortForWorkers;
    public SendPingAliveThread(String uuid, String myPublicIp, List<RGateway> listaGateways, int rmiPortForClientes, int rmiPortForWorkers) {
        this.uuid = uuid;
        this.myPublicIp = myPublicIp;
        this.listaGateways = listaGateways;
        this.rmiPortForClientes = rmiPortForClientes;
        this.rmiPortForWorkers = rmiPortForWorkers;
    }

    @Override
    public void run() {
        while(true) {
            try {
                Thread.sleep(5000);
                connectRandomGatewayRMIForServidor(this.listaGateways).pingAliveFromServidor(this.uuid, this.myPublicIp, this.rmiPortForClientes, this.rmiPortForWorkers);
            } catch (InterruptedException | RemoteException e) {
                log.error("Error: " + e.getMessage());;
            }
        }
    }
}
