package lu.dainesch.homeinflux;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lu.dainesch.homeinflux.out.DataPoint;
import org.influxdb.dto.Point;

/**
 * Base class for creating plugins. Plugins send their point data to the dataQueue 
 * and use the executor to schedule periodic updates
 * 
 */
public abstract class InputPlugin implements Configurable, AutoCloseable {

    private BlockingQueue<DataPoint> dataQueue;
    private ScheduledExecutorService executor;

    void initPlugin(BlockingQueue<DataPoint> dataQueue, ScheduledExecutorService executor) {
        this.dataQueue = dataQueue;
        this.executor = executor;
    }

    public abstract void start();

    protected void addPoint(String dbName, Point point) {
        dataQueue.add(new DataPoint(dbName, point));
    }
    
    protected void schedule(Runnable command, long delay, long interval) {
        executor.scheduleAtFixedRate(command, delay, interval, TimeUnit.MILLISECONDS);
    }

}
