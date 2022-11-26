package blender.distributed.Gateway.Threads;

import blender.distributed.Gateway.Gateway;
import blender.distributed.Records.RWorker;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

public class RefreshListaWorkersThread implements Runnable {
    Logger log = LoggerFactory.getLogger(Gateway.class);
    RedisClient redisPrivClient;
    public RefreshListaWorkersThread(RedisClient redisPrivClient) {
        this.redisPrivClient = redisPrivClient;
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
                throw new RuntimeException(e);
            }
            newListaWorkers = gson.fromJson(String.valueOf(commands.hvals("listaWorkers")), new TypeToken<List<RWorker>>() {
            }.getType());
            listaWorkersToDelete = newListaWorkers.stream().filter(worker -> Duration.between(LocalTime.parse(worker.lastPing()), LocalTime.now()).getSeconds() > 10).collect(Collectors.toList());
            listaWorkersToDelete.stream().map(workerToDelete -> workerToDelete.workerName()).forEach(worker -> {
                commands.hdel("listaWorkers", worker);
                log.info("Worker " + worker + " eliminado por timeout. ");
            });
        }
    }
}
