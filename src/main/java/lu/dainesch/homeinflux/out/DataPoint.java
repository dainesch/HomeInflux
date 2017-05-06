package lu.dainesch.homeinflux.out;

import org.influxdb.dto.Point;

/**
 * Simple DTO to transmit the required data to the influx output
 * 
 */
public class DataPoint {

    private final String database;
    private final Point dataPoint;

    public DataPoint(String database, Point dataPoint) {
        this.database = database;
        this.dataPoint = dataPoint;
    }

    public Point getDataPoint() {
        return dataPoint;
    }

    public String getDatabase() {
        return database;
    }

}
