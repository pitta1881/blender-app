package blender.distributed.gateway.Threads;

import blender.distributed.shared.Enums.EStatus;
import blender.distributed.shared.Records.RParte;
import blender.distributed.shared.Records.RWorker;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;


import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class RefreshListaWorkersThread implements Runnable {
    Logger log;
    RedisClient redisPrivClient;
    public RefreshListaWorkersThread(RedisClient redisPrivClient, Logger log) {
        this.redisPrivClient = redisPrivClient;
        this.log = log;
    }

    @Override
    public void run() {
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
            listaWorkersToDelete = newListaWorkers.stream().filter(worker -> ZonedDateTime.now().toInstant().toEpochMilli() - worker.lastPing() > 10000).collect(Collectors.toList());
            listaWorkersToDelete.stream().map(workerToDelete -> {
                if(workerToDelete.uuidParte() != null) {
                    RParte parteAsociada = gson.fromJson(String.valueOf(commands.hget("listaPartes", workerToDelete.uuidParte())), new TypeToken<RParte>() {
                    }.getType());
                    RParte parteAsociadaUpdated = new RParte(parteAsociada.uuidTrabajo(), parteAsociada.uuid(), parteAsociada.startFrame(), parteAsociada.endFrame(), EStatus.TO_DO, null);
                    commands.hset("listaPartes", parteAsociada.uuid(), gson.toJson(parteAsociadaUpdated));
                    log.info("Redis Priv.: hset listaPartes " + parteAsociada.uuid() + " " + parteAsociadaUpdated);
                }
                return workerToDelete.workerName();
            }).forEach(worker -> {
                commands.hdel("listaWorkers", worker);
                log.info("Redis Priv.: hdel listaWorkers " + worker);
                log.info("Worker " + worker + " eliminado por timeout. ");
            });
        }
    }
}
