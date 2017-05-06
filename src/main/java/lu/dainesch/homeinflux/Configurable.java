package lu.dainesch.homeinflux;

import javax.json.JsonObject;

/**
 * Interface for classes that use the shared config file
 *
 */
public interface Configurable {

    /**
     * key of json config object
     *
     * @return
     */
    String getConfigKey();

    /**
     * Just read the existing config. Can be null
     * @param confObj
     */
    void readConfig(JsonObject confObj);

    /**
     * Checks if the config contains all needed data to start
     * @return 
     */
    boolean hasValidConfig();

    /**
     * Returns a sample config object
     * @return 
     */
    JsonObject getDefaultConfig();

    /**
     * Test config settings that user provided
     * @return 
     */
    boolean testConfig();

}
