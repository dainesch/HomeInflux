package lu.dainesch.homeinflux.input;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import javax.json.Json;
import javax.json.JsonObject;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import lu.dainesch.homeinflux.InputPlugin;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.influxdb.dto.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;

/**
 * Plugin that reads data using the TR064 interface of the fritz box.
 * 
 */
public class FritzMonitorInput extends InputPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(FritzMonitorInput.class);

    private static final String CONFIG_KEY = "fritzbox";
    private static final String SOAP_URL = "/tr064/upnp/control/wancommonifconfig1";
    private static final String SOAP_ACTION = "SOAPAction";
    private static final String SOAP_ACTION_VAL = "urn:dslforum-org:service:WANCommonInterfaceConfig:1#X_AVM-DE_GetOnlineMonitor";
    private static final String SOAP = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
            + "<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<s:Body><u:X_AVM-DE_GetOnlineMonitor xmlns:u=\"urn:dslforum-org:service:WANCommonInterfaceConfig:1\">"
            + "<NewSyncGroupIndex>0</NewSyncGroupIndex>"
            + "</u:X_AVM-DE_GetOnlineMonitor></s:Body></s:Envelope>";
    private static final String RESP_ELEM = "u:X_AVM-DE_GetOnlineMonitorResponse";
    private static final String MAX_DS = "Newmax_ds";
    private static final String MAX_US = "Newmax_us";
    private static final String CURRENT_DS = "Newds_current_bps";
    private static final String CURRENT_US = "Newus_current_bps";
    private static final long INTERVAL = 17 * 5 * 1000;

    private boolean confError = false;
    private String dbName;
    private boolean on = false;
    private String hostname;
    private String user;
    private String pass;

    private CloseableHttpClient httpclient;
    private HttpHost target;
    private HttpClientContext context;
    private HttpPost postRequest;

    private List<Long> lastDS = new ArrayList<>();
    private List<Long> lastUS = new ArrayList<>();



    @Override
    public void start() {
        if (!on) {
            return;
        }

        schedule(this::saveValues, 0, INTERVAL);

    }

    private void saveValues() {
        Map<String, String> values = doRequest();
        long time = System.currentTimeMillis();
        if (values != null) {

            if (values.containsKey(MAX_DS)) {
                Point point = Point.measurement("downstream")
                        .time(time, TimeUnit.MILLISECONDS)
                        .tag("type", "max")
                        .addField("value", Long.parseLong(values.get(MAX_DS)))
                        .build();
                addPoint(dbName, point);
            }
            if (values.containsKey(MAX_US)) {
                Point point = Point.measurement("upstream")
                        .time(time, TimeUnit.MILLISECONDS)
                        .tag("type", "max")
                        .addField("value", Long.parseLong(values.get(MAX_US)))
                        .build();
                addPoint(dbName, point);
            }
            if (values.containsKey(CURRENT_DS)) {
                List<Long> ds = toNumberList(values.get(CURRENT_DS));
                ds = deleteOverlapp(ds, lastDS);
                for (int i = 0; i < ds.size(); i++) {
                    Point point = Point.measurement("downstream")
                            .time(time - i * 5000, TimeUnit.MILLISECONDS)
                            .tag("type", "current")
                            .addField("value", ds.get(i))
                            .build();
                    addPoint(dbName, point);
                }
                lastDS = ds;
            }
            if (values.containsKey(CURRENT_US)) {
                List<Long> us = toNumberList(values.get(CURRENT_US));
                us = deleteOverlapp(us, lastUS);
                for (int i = 0; i < us.size(); i++) {
                    Point point = Point.measurement("upstream")
                            .time(time - i * 5000, TimeUnit.MILLISECONDS)
                            .tag("type", "current")
                            .addField("value", us.get(i))
                            .build();
                    addPoint(dbName, point);
                }
                lastUS = us;
            }
        }
    }

    private List<Long> deleteOverlapp(List<Long> current, List<Long> old) {
        if (old.size() < 2 || current.size() < 4) {
            return current;
        }
        int deleted = 0;
        while (deleted < 4 && !old.get(0).equals(current.get(current.size() - 2)) && !old.get(1).equals(current.get(current.size() - 1))) {
            current.remove(current.size() - 1);
            deleted++;
        }
        
        return current;
    }

    private List<Long> toNumberList(String s) {
        List<Long> ret = new ArrayList<>();
        StringTokenizer tok = new StringTokenizer(s, ",");
        while (tok.hasMoreTokens()) {
            ret.add(Long.parseLong(tok.nextToken()));
        }
        return ret;
    }

    private Map<String, String> doRequest() {
        try (CloseableHttpResponse response = httpclient.execute(target, postRequest, context)) {

            MessageFactory factory = MessageFactory.newInstance();
            SOAPMessage message = factory.createMessage(
                    new MimeHeaders(),
                    new ByteArrayInputStream(EntityUtils.toByteArray(response.getEntity())));

            SOAPBody body = message.getSOAPBody();

            NodeList returnList = body.getElementsByTagName(RESP_ELEM);
            Map<String, String> ret = new HashMap<>();
            extractData(returnList, ret);
            return ret;

        } catch (IOException | SOAPException ex) {
            LOG.error("Error druring request", ex);
            return null;
        }
    }

    public void extractData(NodeList list, Map<String, String> ret) {

        for (int k = 0; k < list.getLength(); k++) {
            NodeList inner = list.item(k).getChildNodes();
            if (inner.getLength() > 1) {
                extractData(inner, ret);
            } else if (inner.getLength() == 1) {
                String name = list.item(k).getNodeName().trim();
                String val = list.item(k).getTextContent().trim();
                if (name.equals(MAX_DS) || name.equals(MAX_US) || name.equals(CURRENT_DS) || name.equals(CURRENT_US)) {
                    ret.put(name, val);
                }
            }
        }

    }

    private void prepare() {
        try {
            SSLContextBuilder builder = new SSLContextBuilder();
            builder.loadTrustMaterial(null, (X509Certificate[] chain, String authType) -> true);
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(),
                    SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            target = new HttpHost(hostname, 443, "https");
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    new AuthScope(target.getHostName(), target.getPort()),
                    new UsernamePasswordCredentials(user, pass));
            httpclient = HttpClients.custom()
                    .setDefaultCredentialsProvider(credsProvider)
                    .setSSLSocketFactory(sslsf)
                    .build();

            AuthCache authCache = new BasicAuthCache();
            DigestScheme digestAuth = new DigestScheme();
            authCache.put(target, digestAuth);

            context = HttpClientContext.create();
            context.setAuthCache(authCache);

            postRequest = new HttpPost("https://" + hostname + SOAP_URL);
            postRequest.addHeader(SOAP_ACTION, SOAP_ACTION_VAL);
            postRequest.setEntity(new StringEntity(SOAP,
                    ContentType.create("text/xml", StandardCharsets.UTF_8)));
        } catch (KeyManagementException | KeyStoreException | NoSuchAlgorithmException ex) {
            target = null;
            httpclient = null;
            postRequest = null;
            LOG.error("Error creating httpclient ", ex);
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
        hostname = confObj.getString("hostname");
        user = confObj.getString("username");
        pass = confObj.getString("password");
    }

    @Override
    public boolean hasValidConfig() {
        if (confError) {
            return false;
        }
        return !on || (on && dbName != null && hostname != null && user != null && pass != null);
    }

    @Override
    public JsonObject getDefaultConfig() {
        return Json.createObjectBuilder()
                .add("on", false)
                .add("database", "fritzbox")
                .add("hostname", "fritz.box")
                .add("username", "admin")
                .add("password", "*****")
                .build();
    }

    @Override
    public boolean testConfig() {
        prepare();
        Map<String, String> values = doRequest();

        return values != null && values.size() > 0;
    }

    @Override
    public void close() throws Exception {
       if (httpclient!=null) {
           httpclient.close();
       }
    }

}
