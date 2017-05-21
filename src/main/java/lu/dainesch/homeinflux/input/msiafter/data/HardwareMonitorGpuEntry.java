package lu.dainesch.homeinflux.input.msiafter.data;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class HardwareMonitorGpuEntry implements Serializable {

    private String gpuId;
    private String family;
    private String device;
    private String driver;
    private String BIOS;
    private String memAmount;

    public String getGpuId() {
        return gpuId;
    }

    public void setGpuId(String gpuId) {
        this.gpuId = gpuId;
    }

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getBIOS() {
        return BIOS;
    }

    public void setBIOS(String BIOS) {
        this.BIOS = BIOS;
    }

    public String getMemAmount() {
        return memAmount;
    }

    public void setMemAmount(String memAmount) {
        this.memAmount = memAmount;
    }
    
    

}
