package blender.distributed.Gateway.Threads;

import blender.distributed.Enums.ENodo;
import blender.distributed.Records.RGateway;
import com.google.gson.Gson;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.time.ZonedDateTime;


public class SendPingAliveThread implements Runnable {
    Logger log;
    RedisClient redisPubClient;
    String uuid;
    String myPublicIp;
    int rmiPortForClientes;
    int rmiPortForWorkers;
    int rmiPortForServidores;

    public SendPingAliveThread(RedisClient redisPubClient, String uuid, String myPublicIp, int rmiPortForClientes, int rmiPortForWorkers, int rmiPortForServidores, Logger log) {
        MDC.put("log.name", ENodo.GATEWAY.name());
        this.redisPubClient = redisPubClient;
        this.uuid = uuid;
        this.myPublicIp = myPublicIp;
        this.rmiPortForClientes = rmiPortForClientes;
        this.rmiPortForWorkers = rmiPortForWorkers;
        this.rmiPortForServidores = rmiPortForServidores;
        this.log = log;
    }

    @Override
    public void run() {
        MDC.put("log.name", ENodo.GATEWAY.name());
        Gson gson = new Gson();
        StatefulRedisConnection redisConnection = redisPubClient.connect();
        RedisCommands commands = redisConnection.sync();
        while(true) {
            try {
                Thread.sleep(5000);
                RGateway recordGateway = new RGateway(this.uuid, this.myPublicIp, this.rmiPortForClientes, this.rmiPortForWorkers, this.rmiPortForServidores, ZonedDateTime.now().toInstant().toEpochMilli());
                String json = gson.toJson(recordGateway);
                commands.hset("listaGateways", this.uuid ,json);
            } catch (InterruptedException e) {
                log.error("Error: " + e.getMessage());;
            }
        }
    }
}
