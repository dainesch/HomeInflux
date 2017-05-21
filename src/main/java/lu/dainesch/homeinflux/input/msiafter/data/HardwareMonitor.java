package lu.dainesch.homeinflux.input.msiafter.data;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "HardwareMonitor")
@XmlAccessorType(XmlAccessType.FIELD)
public class HardwareMonitor implements Serializable {

    @XmlElement(name = "HardwareMonitorHeader")
    private HardwareMonitorHeader header;

    @XmlElementWrapper(name = "HardwareMonitorEntries")
    @XmlElement(name = "HardwareMonitorEntry")
    private List<HardwareMonitorEntry> entries = new LinkedList<>();

    @XmlElementWrapper(name = "HardwareMonitorGpuEntries")
    @XmlElement(name = "HardwareMonitorGpuEntry")
    private List<HardwareMonitorGpuEntry> gpuEntries = new LinkedList<>();

    public HardwareMonitorHeader getHeader() {
        return header;
    }

    public void setHeader(HardwareMonitorHeader header) {
        this.header = header;
    }

    public List<HardwareMonitorEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<HardwareMonitorEntry> entries) {
        this.entries = entries;
    }

    public List<HardwareMonitorGpuEntry> getGpuEntries() {
        return gpuEntries;
    }

    public void setGpuEntries(List<HardwareMonitorGpuEntry> gpuEntries) {
        this.gpuEntries = gpuEntries;
    }
    
    

}
