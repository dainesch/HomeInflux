package lu.dainesch.homeinflux;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConfigManager {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigManager.class);
    private static final String DEFAULT_CONFIG = "./homeinflux.json";

    private final Path configFile;
    private final Map<String, JsonObject> newConfMap;
    private JsonObject config;

    public ConfigManager(Path file) {
        if (file == null || Files.notExists(file)) {
            this.configFile = Paths.get(DEFAULT_CONFIG);
        } else {
            this.configFile = file;
        }
        this.newConfMap = new HashMap<>();
    }

    public void readConfig() throws IOException {
        if (Files.notExists(configFile)) {
            config = Json.createObjectBuilder().build();
            return;
        }
        LOG.info("Reading config file: " + configFile.toString());
        try (InputStream in = Files.newInputStream(configFile)) {
            config = Json.createReader(in).readObject();
        }
    }

    public void writeConfig() throws IOException {
        Files.deleteIfExists(configFile);
        LOG.info("Writing config file: " + configFile.toString());

        JsonObjectBuilder newConf = Json.createObjectBuilder();
        // copy old
        config.entrySet().forEach((entry) -> {
            newConf.add(entry.getKey(), entry.getValue());
        });
        // add new
        newConfMap.entrySet().forEach((entry) -> {
            newConf.add(entry.getKey(), entry.getValue());
        });

        Map<String, Object> properties = new HashMap<>(1);
        properties.put(JsonGenerator.PRETTY_PRINTING, true);

        JsonWriterFactory writerFactory = Json.createWriterFactory(properties);
        try (JsonWriter writer = writerFactory.createWriter(Files.newBufferedWriter(configFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE))) {

            config = newConf.build();
            writer.writeObject(config);
        }
    }

    public JsonObject getConfig(String key) {
        if (config.containsKey(key)) {
            return config.getJsonObject(key);
        }
        return null;
    }

    public void setConfig(String key, JsonObject config) {
        newConfMap.put(key, config);
    }

}
