package lu.dainesch.homeinflux.input;

import java.io.IOException;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import lu.dainesch.homeinflux.InputPlugin;
import org.influxdb.dto.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin that pings a given list of hosts
 * 
 */
public class PingInput extends InputPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(PingInput.class);

    private static final String CONFIG_KEY = "ping";

    private final List<String> servers = new LinkedList<>();

    private boolean confError = false;
    private String dbName;
    private boolean on = false;
    private Long interval;

    @Override
    public void start() {
        if (!on) {
            return;
        }

        servers.forEach((server) -> {
            schedule(new PingTask(server), interval, interval);
        });
    }

    @Override
    public String getConfigKey() {
        return CONFIG_KEY;
    }

    @Override
    public void readConfig(JsonObject confObj) {
        if (confObj == null) {
            confError = true;
            return;
        }
        on = confObj.getBoolean("on", false);
        dbName = confObj.getString("database", null);
        interval = confObj.getJsonNumber("interval").longValue();
        JsonArray arr = confObj.getJsonArray("servers");
        if (arr != null) {
            for (int i = 0; i < arr.size(); i++) {
                servers.add(arr.getString(i));
            }
        }
    }

    @Override
    public boolean hasValidConfig() {
        if (confError) {
            return false;
        }
        return !on || (on && dbName != null && interval != null && !servers.isEmpty());
    }

    @Override
    public JsonObject getDefaultConfig() {
        return Json.createObjectBuilder()
                .add("on", true)
                .add("database", "ping")
                .add("interval", 5000)
                .add("servers", Json.createArrayBuilder().add("google.com").add("localhost"))
                .build();
    }

    @Override
    public boolean testConfig() {
        return true;
    }

    @Override
    public void close() throws Exception {

    }

    private class PingTask extends TimerTask {

        private final String server;

        public PingTask(String server) {
            this.server = server;
        }

        @Override
        public void run() {
            try {
                InetAddress adr = InetAddress.getByName(server);

                long start = System.currentTimeMillis();
                if (adr.isReachable(interval.intValue() - 100)) {
                    long time = System.currentTimeMillis() - start;

                    Point point = Point.measurement("ping")
                            .time(start, TimeUnit.MILLISECONDS)
                            .tag("server", server)
                            .addField("value", time)
                            .build();
                    addPoint(dbName, point);
                }
            } catch (IOException ex) {
                LOG.error("Error pinging server " + server, ex);
            }
        }

    }
}
