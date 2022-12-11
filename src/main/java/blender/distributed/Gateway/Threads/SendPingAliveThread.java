package blender.distributed.Gateway.Threads;

import blender.distributed.Records.RGateway;
import com.google.gson.Gson;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalTime;


public class SendPingAliveThread implements Runnable {
    static Logger log = LoggerFactory.getLogger(SendPingAliveThread.class);
    RedisClient redisPubClient;
    String uuid;
    String myPublicIp;
    int rmiPortForClientes;
    int rmiPortForWorkers;
    int rmiPortForServidores;

    public SendPingAliveThread(RedisClient redisPubClient, String uuid, String myPublicIp, int rmiPortForClientes, int rmiPortForWorkers, int rmiPortForServidores) {
        this.redisPubClient = redisPubClient;
        this.uuid = uuid;
        this.myPublicIp = myPublicIp;
        this.rmiPortForClientes = rmiPortForClientes;
        this.rmiPortForWorkers = rmiPortForWorkers;
        this.rmiPortForServidores = rmiPortForServidores;
    }

    @Override
    public void run() {
        Gson gson = new Gson();
        StatefulRedisConnection redisConnection = redisPubClient.connect();
        RedisCommands commands = redisConnection.sync();
        while(true) {
            try {
                Thread.sleep(5000);
                RGateway recordGateway = new RGateway(this.uuid, this.myPublicIp, this.rmiPortForClientes, this.rmiPortForWorkers, this.rmiPortForServidores, LocalTime.now().toString());
                String json = gson.toJson(recordGateway);
                commands.hset("listaGateways", this.uuid ,json);
            } catch (InterruptedException e) {
                log.error("Error: " + e.getMessage());;
            }
        }
    }
}
