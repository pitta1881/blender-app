package blender.distributed.gateway.Threads;

import blender.distributed.shared.Records.RGateway;
import com.google.gson.Gson;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;


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
        Gson gson = new Gson();
        StatefulRedisConnection redisConnection = redisPubClient.connect();
        RedisCommands commands = redisConnection.sync();
        while(true) {
            try {
                Thread.sleep(5000);
                RGateway recordGateway = new RGateway(this.uuid, this.myPublicIp, this.rmiPortForClientes, this.rmiPortForWorkers, this.rmiPortForServidores, ZonedDateTime.now().toInstant().toEpochMilli());
                String json = gson.toJson(recordGateway);
                commands.hset("listaGateways", this.uuid ,json);
                //log.info("Redis Pub.: hset listaGateways " + this.uuid + " " + recordGateway); // too much flood
            } catch (InterruptedException e) {
                log.error("Error: " + e.getMessage());;
            }
        }
    }
}
