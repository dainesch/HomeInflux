package lu.dainesch.homeinflux.input.hue;

import java.io.IOException;
import javax.json.Json;
import javax.json.JsonObject;
import lu.dainesch.homeinflux.InputPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HueSensorInput extends InputPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(HueSensorInput.class);

    private static final String CONFIG_KEY = "hue";

    private boolean confError = false;
    private String dbName;
    private boolean on = false;
    private Long interval;

    private String bridgeIp;
    private String bridgeKey;

    private HueComm hue;

    @Override
    public void start() {
        if (!on) {
            return;
        }

        schedule(this::readValues, interval, interval);

    }

    private void readValues() {
        try {
            hue.updateSensors().forEach(p -> addPoint(dbName, p));

        } catch (IOException ex) {
            LOG.error("Error reading sensor values from hue", ex);
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

        bridgeIp = confObj.getString("bridgeIp", null);
        bridgeKey = confObj.getString("bridgeKey", null);
    }

    @Override
    public boolean hasValidConfig() {
        if (confError) {
            return false;
        }
        return !on || (on && dbName != null && interval != null && bridgeIp != null);
    }

    @Override
    public JsonObject getDefaultConfig() {
        return Json.createObjectBuilder()
                .add("on", false)
                .add("database", "hue")
                .add("interval", 30000)
                .add("bridgeIp", "127.0.0.1")
                .add("bridgeKey", "If key is not set, a new key wil be requested on start")
                .build();
    }

    @Override
    public boolean testConfig() {
        if (!on) {
            return true;
        }
        try {
            hue = new HueComm(bridgeIp, bridgeKey);
            if (!hue.testConnection()) {
                LOG.error("Error connecting to Hue Bridge! Check given IP in config");
                return false;
            }
            if (!hue.testAuth()) {
                LOG.info("Could not auth, trying to retrieve key ....");
                LOG.info("--------------------------------------");
                LOG.info("- PRESS THE BUTTON ON THE HUE BRIDGE -");
                LOG.info("--------------------------------------");
                if (hue.waitForAuth()) {
                    LOG.info("--------------------------------------------------");
                    LOG.info("- GOT KEY FROM BRIDGE, PLEASE UPDATE YOUR CONFIG -");
                    LOG.info("KEY: " + hue.getBridgeKey());
                    LOG.info("--------------------------------------------------");
                } else {
                    LOG.info("Could not retrieve key, aborting");
                }
                return false;
            } else {
                return true;
            }
        } catch (IOException | InterruptedException ex) {
            LOG.info("Error while testing connection with hue", ex);
        }
        return false;
    }

    @Override
    public void close() throws Exception {

    }

}
