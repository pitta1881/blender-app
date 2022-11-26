package blender.distributed.Servidor.Threads;

import blender.distributed.Records.RGateway;

import java.rmi.RemoteException;
import java.util.List;

import static blender.distributed.Gateway.Tools.connectRandomGatewayRMIForServidor;

public class SendPingAliveThread implements Runnable {
    List<RGateway> listaGateways;
    String uuid;
    int rmiPortForClientes;
    int rmiPortForWorkers;
    public SendPingAliveThread(String uuid, List<RGateway> listaGateways, int rmiPortForClientes, int rmiPortForWorkers) {
        this.uuid = uuid;
        this.listaGateways = listaGateways;
        this.rmiPortForClientes = rmiPortForClientes;
        this.rmiPortForWorkers = rmiPortForWorkers;
    }

    @Override
    public void run() {
        while(true) {
            try {
                Thread.sleep(5000);
                connectRandomGatewayRMIForServidor(this.listaGateways).pingAliveFromServidor(this.uuid, this.rmiPortForClientes, this.rmiPortForWorkers);
            } catch (InterruptedException | RemoteException e) {
            }
        }
    }
}
