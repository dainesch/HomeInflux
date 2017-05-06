package lu.dainesch.homeinflux;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import lu.dainesch.homeinflux.out.DataPoint;
import lu.dainesch.homeinflux.out.InfluxOutput;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Launcher {

    private static final Logger LOG = LoggerFactory.getLogger(Launcher.class);

    public static void main(String[] args) {

        Path configFile = null;
        if (args.length == 1) {
            if ("--help".equals(args[0]) || "-h".equals(args[0])) {
                LOG.info("Usage: java -jar HomeInflux.jar /path/to/config/config.json");
                LOG.info("The config file will be generated on first start. Verify the settings before the second start!");
                return;
            }
            configFile = Paths.get(args[0]);
        }

        // shared queue to transmit the collected data
        BlockingQueue<DataPoint> dataQueue = new LinkedBlockingQueue<>();
        // executor to schedule data sending/retrieving
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(2, new ThreadFactory() {
            private int count = 0;

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "ScheduleThread-" + count++);
            }
        });

        List<Configurable> configurables = new ArrayList<>();

        InfluxOutput out = new InfluxOutput(dataQueue, executor);
        configurables.add(out);

        List<InputPlugin> plugins = loadPlugins();
        plugins.forEach(p -> {
            configurables.add(p);
            p.initPlugin(dataQueue, executor);
        });

        // config
        handleConfig(configFile, configurables);

        // at least try orderly shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("");
            executor.shutdown();
            try {
                LOG.info("Stopping outpout....");
                out.close();
            } catch (Exception ex) {
                LOG.error("Error while stopping output", ex);
            }
            plugins.forEach(p -> {
                try {
                    LOG.info("Stopping plugin " + p.getConfigKey() + "....");
                    p.close();
                } catch (Exception ex) {
                    LOG.error("Error while stopping plugin " + p.getConfigKey(), ex);
                }
            });
        }));

        // starting ...
        out.start();
        plugins.forEach(p -> p.start());
    }

    /**
     * Load all plugins extending the {@link InputPlugin} class
     * @return plugins
     */
    private static List<InputPlugin> loadPlugins() {
        List<InputPlugin> ret = new LinkedList<>();
        Reflections reflections = new Reflections(Launcher.class.getPackage().getName());
        Set<Class<? extends InputPlugin>> plugins
                = reflections.getSubTypesOf(InputPlugin.class);
        for (Class<? extends InputPlugin> p : plugins) {
            boolean hasDefConst = false;
            for (Constructor<?> con : p.getDeclaredConstructors()) {
                if (con.getParameterCount() == 0) {
                    hasDefConst = true;
                }
            }
            if (!hasDefConst) {
                LOG.error("InputPlugin does not have a default constructor: " + p.getName());
            } else {
                try {
                    InputPlugin plugin = p.newInstance();
                    ret.add(plugin);
                    LOG.info("Loading plugin " + p.getSimpleName());
                } catch (IllegalAccessException | InstantiationException ex) {
                    LOG.error("Error instanciating plugin " + p.getName());
                }
            }
        }
        return ret;
    }

    private static void handleConfig(Path configFile, List<Configurable> configurables) {
        ConfigManager config = new ConfigManager(configFile);

        try {
            config.readConfig();
        } catch (IOException ex) {
            LOG.error("Error reading config file!", ex);
            System.exit(1);
        }

        // let the plugins read their config and ask them if it's ok.
        // if not, ask the plugins for their default config
        boolean allOk = true;
        for (Configurable c : configurables) {
            String key = c.getConfigKey();
            c.readConfig(config.getConfig(key));
            if (!c.hasValidConfig()) {
                allOk = false;
                LOG.warn(key + " does not have a valid config! Adding default config...");
                config.setConfig(key, c.getDefaultConfig());
            }
        }

        // update the config file with the default plugin configs and exit
        if (!allOk) {
            try {
                config.writeConfig();
                System.exit(0);
            } catch (IOException ex) {
                LOG.error("Error writing config file!", ex);
                System.exit(1);
            }
        }

        // let the plugins test the config settings, such as server ips and usernames ...
        allOk = true;
        for (Configurable c : configurables) {
            if (!c.testConfig()) {
                allOk = false;
                LOG.warn("Error while testing config for " + c.getConfigKey());
            }
        }

        if (!allOk) {
            System.exit(1);
        }
    }

}
