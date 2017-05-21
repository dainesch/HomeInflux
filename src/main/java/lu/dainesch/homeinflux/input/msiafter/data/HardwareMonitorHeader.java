
package lu.dainesch.homeinflux.input.msiafter.data;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class HardwareMonitorHeader implements Serializable {
    
    private String signature;
    private String version;
    private int headerSize;
    private int entryCount;
    private long entrySize;
    private long time;
    private int gpuEntryCount;
    private long gpuEntrySize;

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getHeaderSize() {
        return headerSize;
    }

    public void setHeaderSize(int headerSize) {
        this.headerSize = headerSize;
    }

    public int getEntryCount() {
        return entryCount;
    }

    public void setEntryCount(int entryCount) {
        this.entryCount = entryCount;
    }

    public long getEntrySize() {
        return entrySize;
    }

    public void setEntrySize(long entrySize) {
        this.entrySize = entrySize;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getGpuEntryCount() {
        return gpuEntryCount;
    }

    public void setGpuEntryCount(int gpuEntryCount) {
        this.gpuEntryCount = gpuEntryCount;
    }

    public long getGpuEntrySize() {
        return gpuEntrySize;
    }

    public void setGpuEntrySize(long gpuEntrySize) {
        this.gpuEntrySize = gpuEntrySize;
    }
    
    
    
}
