package blender.distributed.SharedTools;

import blender.distributed.Records.RGateway;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.lang.reflect.Type;
import java.util.List;

public class RefreshListaGatewaysThread implements Runnable{
    Logger log;
    List<RGateway> listaGateways;
    RedisClient redisPubClient;
    Type RListaGatewayType = new TypeToken<List<RGateway>>(){}.getType();
    public RefreshListaGatewaysThread(List<RGateway> listaGateways, RedisClient redisPubClient, Logger log, String logName) {
        MDC.put("log.name", logName);
        this.listaGateways = listaGateways;
        this.redisPubClient = redisPubClient;
        this.log = log;
    }

    @Override
    public void run() {
        StatefulRedisConnection redisConnection = this.redisPubClient.connect();
        RedisCommands commands = redisConnection.sync();
        Gson gson = new Gson();
        List<RGateway> newListaGateways;
        while (true){
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                log.error("Error: " + e.getMessage());
            }
            synchronized (this.listaGateways) {
                newListaGateways = gson.fromJson(String.valueOf(commands.hvals("listaGateways")), RListaGatewayType);
                this.listaGateways.clear();
                this.listaGateways.addAll(newListaGateways);
            }
        }
    }
}
