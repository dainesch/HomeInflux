package lu.dainesch.homeinflux.input.snmp;

import java.io.IOException;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;


class SNMPClient implements AutoCloseable {

    private final Snmp snmp;
    private final TransportMapping transport;

    SNMPClient() throws IOException {
        transport = new DefaultUdpTransportMapping();
        snmp = new Snmp(transport);
        transport.listen();

    }

    public String getAsString(SNMPQuery query) throws IOException {
        OID toid = new OID(query.getOid());
        ResponseEvent event = get(toid, "udp:" + query.getServer() + "/161", query.getCommunity());
        return event.getResponse().get(0).getVariable().toString();
    }

    private ResponseEvent get(OID oid, String address, String community) throws IOException {
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(oid));

        Address targetAddress = GenericAddress.parse(address);
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString(community));
        target.setAddress(targetAddress);
        target.setRetries(2);
        target.setTimeout(1500);
        target.setVersion(SnmpConstants.version2c);

        pdu.setType(PDU.GET);
        ResponseEvent event = snmp.send(pdu, target, transport);
        if (event != null) {
            return event;
        }
        throw new RuntimeException("GET timed out");
    }

    @Override
    public void close() throws Exception {
        snmp.close();
        transport.close();
    }

}
