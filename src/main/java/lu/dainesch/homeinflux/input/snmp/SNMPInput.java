package lu.dainesch.homeinflux.input.snmp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import lu.dainesch.homeinflux.InputPlugin;
import org.influxdb.dto.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin that reads data from snmp
 */
public class SNMPInput extends InputPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(SNMPInput.class);

    private static final String CONFIG_KEY = "snmp";

    private boolean confError = false;
    private String dbName;
    private boolean on = false;
    private Long interval;
    private SNMPClient client;
    private final List<SNMPQuery> queries = new ArrayList<>();

    @Override
    public void start() {
        if (!on) {
            return;
        }
        try {
            client = new SNMPClient();
        } catch (IOException ex) {
            LOG.error("Error creating SNMP client!", ex);
            return;
        }

        schedule(this::makeQueries, interval, interval);

    }

    private void makeQueries() {
        for (SNMPQuery q : queries) {
            try {
                String value = client.getAsString(q);
                Long val = null;
                try {
                    val = Long.parseLong(value);
                } catch (NumberFormatException ex) {

                }
                if (val != null) {
                    Point point = Point.measurement(q.getMeasurement())
                            .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                            .addField("value", val)
                            .build();
                    addPoint(dbName, point);
                }
            } catch (IOException ex) {
                LOG.error("Error executing query: " + q, ex);
            }
        }
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
        JsonArray arr = confObj.getJsonArray("queries");
        for (int i = 0; i < arr.size(); i++) {
            queries.add(new SNMPQuery(arr.getJsonObject(i)));
        }
    }

    @Override
    public boolean hasValidConfig() {
        if (confError) {
            return false;
        }
        boolean qValid = true;
        for (SNMPQuery q : queries) {
            qValid = qValid && q.valid();
        }
        return !on || (on && dbName != null && interval != null && !queries.isEmpty() && qValid);
    }

    @Override
    public JsonObject getDefaultConfig() {
        return Json.createObjectBuilder()
                .add("on", false)
                .add("database", "snmp")
                .add("interval", 5000)
                .add("queries",
                        Json.createArrayBuilder().add(
                                Json.createObjectBuilder()
                                        .add("ip", "127.0.0.1")
                                        .add("community", "public")
                                        .add("oid", ".1.3.6.1.2.1.25.3.3.1.2.1")
                                        .add("measurement", "cpu_1")
                        ).add(
                                Json.createObjectBuilder()
                                        .add("ip", "127.0.0.1")
                                        .add("community", "public")
                                        .add("oid", ".1.3.6.1.2.1.25.3.3.1.2.2")
                                        .add("measurement", "cpu_2")
                        ))
                .build();
    }

    @Override
    public boolean testConfig() {
        return true;
    }

    @Override
    public void close() throws Exception {
        if (client != null) {
            client.close();
        }
    }

}
