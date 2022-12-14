package blender.distributed;

import blender.distributed.Records.RGateway;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.cdimascio.dotenv.Dotenv;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.Type;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class CleanListaGateways {
    Logger log = LoggerFactory.getLogger(this.getClass());
    Dotenv dotenv = Dotenv.load();
    RedisClient redisPubClient;
    Type RListaGatewayType = new TypeToken<List<RGateway>>(){}.getType();

    public CleanListaGateways(){
        MDC.put("log.name", "CleanListaGateways");
        connectRedisPub();
        cleanListaGateway();
    }

    private void connectRedisPub(){
        String redisPubWriteURI = "redis://"+dotenv.get("REDIS_PUBLIC_WRITE_USER")+":"+dotenv.get("REDIS_PUBLIC_WRITE_PASS")+"@"+dotenv.get("REDIS_PUBLIC_IP")+":"+dotenv.get("REDIS_PUBLIC_PORT");
        this.redisPubClient = RedisClient.create(redisPubWriteURI);
    }

    public void cleanListaGateway() {
        StatefulRedisConnection redisConnection = redisPubClient.connect();
        log.info("Conectado a Redis Público exitosamente.");
        RedisCommands commands = redisConnection.sync();
        Gson gson = new Gson();
        List<RGateway> newListaGateway;
        List<RGateway> listaGatewayToDelete;
        while(true) {
            newListaGateway = gson.fromJson(String.valueOf(commands.hvals("listaGateways")), RListaGatewayType);
            listaGatewayToDelete = newListaGateway.stream().filter(gateway ->
                 ZonedDateTime.now().toInstant().toEpochMilli() - gateway.lastPing() > 10000
            ).collect(Collectors.toList());
            listaGatewayToDelete.stream().map(gatewToDelete -> gatewToDelete.uuid()).forEach(gatew -> {
                commands.hdel("listaGateways", gatew);
                log.info("Gateway " + gatew + " eliminado por timeout. ");
            });
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                log.error("Error: " + e.getMessage());
            }
        }
    }

    public static void main (String[] args) {
        new CleanListaGateways();
    }
}
