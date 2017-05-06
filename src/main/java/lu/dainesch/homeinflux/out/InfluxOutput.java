package lu.dainesch.homeinflux.out;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.json.Json;
import javax.json.JsonObject;
import lu.dainesch.homeinflux.Configurable;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.ConsistencyLevel;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Pong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Batch outputs all data to the influxdb
 *
 */
public class InfluxOutput implements Configurable, Runnable, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(InfluxOutput.class);

    private static final String CONFIG_KEY = "influxdb";

    private final BlockingQueue<DataPoint> dataQueue;
    private final ScheduledExecutorService executor;

    private String serverURL;
    private String user;
    private String pass;
    private Long pushInterval;

    private InfluxDB db;

    public InfluxOutput(BlockingQueue<DataPoint> dataQueue, ScheduledExecutorService executor) {
        this.dataQueue = dataQueue;
        this.executor = executor;
    }

    public void start() {
        executor.scheduleAtFixedRate(this, pushInterval, pushInterval, TimeUnit.MILLISECONDS);
    }

    @Override
    public void run() {
        if (dataQueue.isEmpty()) {
            return;
        }
        Map<String, List<Point>> dbPointMap = new HashMap<>();
        List<DataPoint> allPoints = new ArrayList<>(dataQueue.size());
        dataQueue.drainTo(allPoints);
        allPoints.forEach(p -> {
            if (!dbPointMap.containsKey(p.getDatabase())) {
                dbPointMap.put(p.getDatabase(), new ArrayList<>());
            }
            dbPointMap.get(p.getDatabase()).add(p.getDataPoint());
        });

        for (Entry<String, List<Point>> e : dbPointMap.entrySet()) {
            String dbName = e.getKey();
            List<Point> points = e.getValue();

            db.createDatabase(dbName);

            BatchPoints batchPoints = BatchPoints
                    .database(dbName)
                    .retentionPolicy("autogen")
                    .consistency(ConsistencyLevel.ALL)
                    .build();
            batchPoints.getPoints().addAll(points);
            db.write(batchPoints);
        }
    }

    private boolean connect() {
        try {
            db = InfluxDBFactory.connect(serverURL, user, pass);
            Pong pong = db.ping();
            LOG.info("Connected to influx. Version: " + pong.getVersion() + " Latency: " + pong.getResponseTime());

            return true;
        } catch (Exception ex) {
            // this api is .. no good
            LOG.error("Error connecting to db", ex);
        }
        return false;
    }

    @Override
    public String getConfigKey() {
        return CONFIG_KEY;
    }

    @Override
    public void readConfig(JsonObject confObj) {

        if (confObj == null) {
            return;
        }
        serverURL = confObj.getString("serverURL", null);
        user = confObj.getString("user", null);
        pass = confObj.getString("pass", null);

        pushInterval = confObj.getJsonNumber("pushInterval").longValue();
    }

    @Override
    public boolean hasValidConfig() {
        return serverURL != null && user != null && pass != null;
    }

    @Override
    public JsonObject getDefaultConfig() {
        return Json.createObjectBuilder()
                .add("serverURL", "http://influxhost:8086")
                .add("user", "username")
                .add("pass", "password")
                .add("pushInterval", 5000)
                .build();
    }

    @Override
    public boolean testConfig() {

        return connect();
    }

    @Override
    public void close() throws Exception {
        db.close();
    }

}
