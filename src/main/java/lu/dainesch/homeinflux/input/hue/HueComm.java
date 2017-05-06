package lu.dainesch.homeinflux.input.hue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import javax.json.JsonReader;
import org.influxdb.dto.Point;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HueComm {

    private static final Logger LOG = LoggerFactory.getLogger(HueComm.class);

    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final String PUT = "PUT";

    private static final String PROTOCOL = "http://";
    private static final String DESCRIPTION = "/description.xml";
    private static final String API = "/api";
    private static final String SENSORS = "/sensors";
    private static final String IDENTBODY = "{\"devicetype\":\"homeinflux#app\"}";

    private final String bridgeIP;
    private String bridgeKey;

    HueComm(String bridgeIP, String bridgeKey) {
        this.bridgeIP = bridgeIP;
        this.bridgeKey = bridgeKey;
    }

    public List<Point> updateSensors() throws IOException {
        List<Point> ret = new LinkedList<>();
        String data = makeQuery(GET, PROTOCOL + bridgeIP + API + "/" + bridgeKey + SENSORS, null);

        JsonObject list = Json.createReader(new StringReader(data)).readObject();
        for (String sId : list.keySet()) {
            Integer id = Integer.parseInt(sId);

            JsonObject sensObj = list.getJsonObject(sId);
            String sType = sensObj.getString("type");
            SensorType type = SensorType.getByType(sType);

            if (type != null) {
                // only supported types

                switch (type) {
                    case LIGHT:
                        updateLightSensor(id, sensObj, ret);
                        break;
                    case PRESENCE:
                        updatePresenceSensor(id, sensObj, ret);
                        break;
                    case TEMPERATURE:
                        updateTempSensor(id, sensObj, ret);
                        break;
                }

            }
        }
        return ret;

    }

    private void updateLightSensor(int id, JsonObject obj, List<Point> points) {
        try {
            String name = obj.getString("name");

            JsonObject state = obj.getJsonObject("state");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            Date time = sdf.parse(state.getString("lastupdated"));

            BigDecimal lightlevel = toLux(state.getInt("lightlevel"));
            boolean dark = state.getBoolean("dark");
            boolean day = state.getBoolean("daylight");

            JsonObject conf = obj.getJsonObject("config");

            boolean on = conf.getBoolean("on");
            int batt = conf.getInt("battery");
            boolean reachable = conf.getBoolean("reachable");

            points.add(Point.measurement(name)
                    .time(time.getTime(), TimeUnit.MILLISECONDS)
                    .tag("type", "light")
                    .tag("id", String.valueOf(id))
                    .addField("lux", lightlevel)
                    .addField("dark", dark)
                    .addField("day", day)
                    .addField("on", on)
                    .addField("battery", batt)
                    .addField("reachable", reachable)
                    .build());

        } catch (ParseException | NullPointerException | ClassCastException ex) {
            LOG.error("Error reading light sensor", ex);
        }
    }

    private void updatePresenceSensor(int id, JsonObject obj, List<Point> points) {
        try {
            String name = obj.getString("name");
            JsonObject state = obj.getJsonObject("state");

            Boolean pres = state.getBoolean("presence");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            //Date time = sdf.parse(state.getString("lastupdated"));

            JsonObject conf = obj.getJsonObject("config");
            boolean on = conf.getBoolean("on");
            int batt = conf.getInt("battery");
            boolean reachable = conf.getBoolean("reachable");

            // to view presence as graph, ignore time
            points.add(Point.measurement(name)
                    .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .tag("type", "presence")
                    .tag("id", String.valueOf(id))
                    .addField("detected", pres)
                    .addField("on", on)
                    .addField("battery", batt)
                    .addField("reachable", reachable)
                    .build());

        } catch (NullPointerException | ClassCastException ex) {
            LOG.error("Error reading presence sensor", ex);
        }
    }

    private void updateTempSensor(int id, JsonObject obj, List<Point> points) {
        try {
            String name = obj.getString("name");

            JsonObject state = obj.getJsonObject("state");

            int t = state.getInt("temperature");
            BigDecimal temp = new BigDecimal(t).movePointLeft(2);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            Date time = sdf.parse(state.getString("lastupdated"));

            JsonObject conf = obj.getJsonObject("config");
            boolean on = conf.getBoolean("on");
            int batt = conf.getInt("battery");
            boolean reachable = conf.getBoolean("reachable");

            points.add(Point.measurement(name)
                    .time(time.getTime(), TimeUnit.MILLISECONDS)
                    .tag("type", "temp")
                    .tag("id", String.valueOf(id))
                    .addField("temperature", temp)
                    .addField("on", on)
                    .addField("battery", batt)
                    .addField("reachable", reachable)
                    .build());

        } catch (ParseException | NullPointerException | ClassCastException ex) {
            LOG.error("Error reading temp sensor", ex);
        }
    }

    public boolean waitForAuth() throws InterruptedException, IOException {
        for (int i = 0; i < 100; i++) {

            if (getKey() && testAuth()) {
                return true;
            }
            Thread.sleep(3000);
        }
        return false;
    }

    private boolean getKey() throws IOException {
        String resp = makeQuery(POST, PROTOCOL + bridgeIP + API, IDENTBODY);
        if (resp != null) {
            JsonReader reader = Json.createReader(new StringReader(resp));
            JsonArray arr = reader.readArray();
            JsonObject obj = arr.getJsonObject(0);
            if (obj.containsKey("success")) {
                JsonObject success = obj.getJsonObject("success");
                bridgeKey = success.getString("username");
                return true;
            }

        }
        return false;
    }

    public boolean testAuth() throws IOException {
        if (bridgeIP != null && bridgeKey != null) {
            String res = makeQuery(GET, PROTOCOL + bridgeIP + API + "/" + bridgeKey + SENSORS, null);
            if (res != null && !res.contains("error")) {
                LOG.info("Authenticated on bridge");
                return true;
            }
        }
        return false;
    }

    public boolean testConnection() throws IOException {
        if (bridgeIP != null) {
            if (makeQuery(GET, PROTOCOL + bridgeIP + DESCRIPTION, null) != null) {
                // connected
                LOG.info("Connection established");

                return true;
            }

        }
        return false;
    }

    private String makeQuery(String method, String path, String body) throws IOException {

        StringBuilder result = new StringBuilder();
        URL url = new URL(path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setReadTimeout((int) TimeUnit.SECONDS.toMillis(10));
        if ((method.equals(POST) || method.endsWith(PUT)) && body != null) {
            conn.setDoOutput(true);
            try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8))) {
                w.write(body);
                w.flush();
            }
        }
        if (conn.getResponseCode() == 200) {
            try (BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = rd.readLine()) != null) {
                    result.append(line);
                }
            }
            return result.toString();
        }
        return null;

    }

    private static BigDecimal toLux(int input) {
        // linear interpolation from table in api docs
        double val = (double) input;
        double ret = 0;
        if (val > 1 && val <= 3000) {
            ret = val / 2999d + 2998d / 2999d;
        } else if (val > 3000 && val <= 10000) {
            ret = val / 875d - 10d / 7d;
        } else if (val > 10000 && val <= 17000) {
            ret = val / 175d - 330d / 7d;
        } else if (val > 17000 && val <= 22000) {
            ret = val / 50d - 290d;
        } else if (val > 22000 && val <= 25500) {
            ret = 2d * val / 35d - 7750d / 7d;
        } else if (val > 25500 && val <= 28500) {
            ret = 7d * val / 60d - 2625d;
        } else if (val > 28500 && val <= 33000) {
            ret = 13d * val / 45d - 22600d / 3d;
        } else if (val > 33000 && val <= 40000) {
            ret = 8d * val / 7d - 250000d / 7d;
        } else if (val > 40000) {
            ret = 10d * val - 390000d;
        }
        return new BigDecimal(ret).setScale(2, RoundingMode.HALF_EVEN);
    }

    public String getBridgeKey() {
        return bridgeKey;
    }

}
