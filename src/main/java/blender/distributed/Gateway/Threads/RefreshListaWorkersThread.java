package blender.distributed.Gateway.Threads;

import blender.distributed.Enums.ENodo;
import blender.distributed.Enums.EStatus;
import blender.distributed.Records.RParte;
import blender.distributed.Records.RWorker;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

public class RefreshListaWorkersThread implements Runnable {
    Logger log;
    RedisClient redisPrivClient;
    public RefreshListaWorkersThread(RedisClient redisPrivClient, Logger log) {
        MDC.put("log.name", ENodo.GATEWAY.name());
        this.redisPrivClient = redisPrivClient;
        this.log = log;
    }

    @Override
    public void run() {
        MDC.put("log.name", ENodo.GATEWAY.name());
        List<RWorker> newListaWorkers;
        List<RWorker> listaWorkersToDelete;
        StatefulRedisConnection redisConnection = redisPrivClient.connect();
        RedisCommands commands = redisConnection.sync();
        Gson gson = new Gson();
        while(true) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                log.error("Error: " + e.getMessage());
            }
            newListaWorkers = gson.fromJson(String.valueOf(commands.hvals("listaWorkers")), new TypeToken<List<RWorker>>() {
            }.getType());
            listaWorkersToDelete = newListaWorkers.stream().filter(worker -> Duration.between(LocalTime.parse(worker.lastPing()), LocalTime.now()).getSeconds() > 10).collect(Collectors.toList());
            listaWorkersToDelete.stream().map(workerToDelete -> {
                if(workerToDelete.uuidParte() != null) {
                    RParte parteAsociada = gson.fromJson(String.valueOf(commands.hget("listaPartes", workerToDelete.uuidParte())), new TypeToken<RParte>() {
                    }.getType());
                    RParte parteAsociadaUpdated = new RParte(parteAsociada.uuidTrabajo(), parteAsociada.uuid(), parteAsociada.startFrame(), parteAsociada.endFrame(), EStatus.TO_DO, null);
                    commands.hset("listaPartes", parteAsociada.uuid(), gson.toJson(parteAsociadaUpdated));
                }
                return workerToDelete.workerName();
            }).forEach(worker -> {
                commands.hdel("listaWorkers", worker);
                log.info("Worker " + worker + " eliminado por timeout. ");
            });
        }
    }
}
