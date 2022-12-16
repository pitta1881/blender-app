package blender.distributed.Gateway.Threads;

import blender.distributed.Records.RServidor;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;


import java.lang.reflect.Type;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;


public class RefreshListaServidoresThread implements Runnable {
    Logger log;
    List<RServidor> listaServidores;
    RedisClient redisPrivClient;
    Type RListaServidorType = new TypeToken<List<RServidor>>(){}.getType();

    public RefreshListaServidoresThread(List<RServidor> listaServidores, RedisClient redisPrivClient, Logger log) {
        this.listaServidores = listaServidores;
        this.redisPrivClient = redisPrivClient;
        this.log = log;
    }

    @Override
    public void run() {
        StatefulRedisConnection redisConnection = redisPrivClient.connect();
        RedisCommands commands = redisConnection.sync();
        Gson gson = new Gson();
        List<RServidor> newListaServidores;
        List<RServidor> listaServidoresToDelete;
        while(true) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                log.error("Error: " + e.getMessage());
            }
            synchronized (this.listaServidores) {
                newListaServidores = gson.fromJson(String.valueOf(commands.hvals("listaServidores")), RListaServidorType);
                listaServidoresToDelete = newListaServidores.stream().filter(servidor -> ZonedDateTime.now().toInstant().toEpochMilli() - servidor.lastPing() > 10000).collect(Collectors.toList());
                listaServidoresToDelete.stream().map(servToDelete -> servToDelete.uuid()).forEach(serv -> {
                    commands.hdel("listaServidores", serv);
                    log.info("Redis Priv.: hdel listaServidores " + serv);
                    log.info("Servidor " + serv + " eliminado por timeout. ");
                });
                newListaServidores = newListaServidores.stream().filter(servidor -> ZonedDateTime.now().toInstant().toEpochMilli() - servidor.lastPing() < 10000).collect(Collectors.toList());
                this.listaServidores.clear();
                this.listaServidores.addAll(newListaServidores);
            }
        }
    }
}
