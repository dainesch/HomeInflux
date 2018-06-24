package lu.dainesch.homeinflux.input.msiafter;

import java.util.concurrent.TimeUnit;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import lu.dainesch.homeinflux.InputPlugin;
import lu.dainesch.homeinflux.input.msiafter.data.HardwareMonitor;
import lu.dainesch.homeinflux.input.msiafter.data.HardwareMonitorEntry;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.influxdb.dto.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads data from MSI Afterburner remote server
 */
public class AfterburnerInput extends InputPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(AfterburnerInput.class);

    private static final String CONFIG_KEY = "afterburner";
    private static final String USERNAME = "MSIAfterburner";
    private static final String DEFAULT_KEY = "17cc95b4017d496f82";
    private static final String MAHM = "/mahm";

    private boolean confError = false;
    private String dbName;
    private boolean on = false;
    private Long interval;
    private String host;
    private int port;
    private String key;

    private Client client;
    private WebTarget target;

    @Override
    public void start() {
        if (!on) {
            return;
        }
        schedule(this::saveValues, 0, interval);
    }

    private void saveValues() {
        try {
            Response resp = target.request(MediaType.APPLICATION_XML_TYPE).get();
            HardwareMonitor monitor = resp.readEntity(HardwareMonitor.class);

            long time = monitor.getHeader().getTime() * 1000;

            for (HardwareMonitorEntry entry : monitor.getEntries()) {
                Point point = Point.measurement("HWMonitor")
                        .time(time, TimeUnit.MILLISECONDS)
                        .tag("src", entry.getSrcName())
                        .tag("gpu", entry.getGpu()+"")
                        .addField("value", entry.getData())
                        .addField("minLimit", entry.getMinLimit())
                        .addField("maxLimit", entry.getMaxLimit())
                        .build();
                addPoint(dbName, point);
            }

        } catch (Exception ex) {
            // ignore
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
        port = confObj.getJsonNumber("port").intValue();
        host = confObj.getString("host", null);
        key = confObj.getString("key", null);

    }

    @Override
    public boolean hasValidConfig() {
        if (confError) {
            return false;
        }
        return !on || (on && dbName != null && interval != null
                && port > 0 && host != null && !host.isEmpty()
                && key != null && !key.isEmpty());
    }

    @Override
    public JsonObject getDefaultConfig() {
        return Json.createObjectBuilder()
                .add("on", true)
                .add("database", "afterburner")
                .add("interval", 5000)
                .add("host", "localhost")
                .add("port", 82)
                .add("key", DEFAULT_KEY)
                .build();
    }

    @Override
    public boolean testConfig() {
        if (!on) {
            return true;
        }
        HttpAuthenticationFeature digestAuth = HttpAuthenticationFeature.universal(USERNAME, key);

        client = ClientBuilder.newClient();
        client.register(digestAuth);
        // fix wrong response header
        client.register((ClientResponseFilter) (requestContext, responseContext)
                -> responseContext.getHeaders().putSingle("Content-Type", MediaType.APPLICATION_XML));
        //client.register(new LoggingFeature(java.util.logging.Logger.getAnonymousLogger(), Level.INFO,null,null));
        target = client.target("http://" + host + ":" + port + MAHM);

        try {
            Response resp = target.request(MediaType.APPLICATION_XML_TYPE).get();
            // success or not lauched
            return resp.getStatus() == 200 || resp.getStatus() == 503;

        } catch (Exception ex) {
            LOG.error("Error querying Afterburner", ex);
            return false;
        }

    }

    @Override
    public void close() throws Exception {
        client.close();
    }

}
