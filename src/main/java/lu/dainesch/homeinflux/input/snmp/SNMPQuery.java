package lu.dainesch.homeinflux.input.snmp;

import javax.json.JsonObject;

class SNMPQuery {

    private final String server;
    private final String community;
    private final String oid;

    private final String measurement;

    SNMPQuery(JsonObject obj) {
        server = obj.getString("ip");
        community = obj.getString("community");
        oid = obj.getString("oid");
        measurement = obj.getString("measurement");
    }

    public boolean valid() {
        return server != null && community != null && oid != null && measurement != null;
    }

    public String getServer() {
        return server;
    }

    public String getCommunity() {
        return community;
    }

    public String getOid() {
        return oid;
    }

    public String getMeasurement() {
        return measurement;
    }

    @Override
    public String toString() {
        return "SNMPQuery{" + "server=" + server + ", oid=" + oid + ", measurement=" + measurement + '}';
    }
    
    

}
