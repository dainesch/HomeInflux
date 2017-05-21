package lu.dainesch.homeinflux.input.msiafter.data;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class HardwareMonitorEntry implements Serializable {

    private String srcName;
    private String srcUnits;
    private String localizedSrcName;
    private String localizedSrcUnits;
    private String recommendedFormat;
    private int data;
    private int minLimit;
    private int maxLimit;
    private String flags;
    private int gpu;
    private int srcId;

    public String getSrcName() {
        return srcName;
    }

    public void setSrcName(String srcName) {
        this.srcName = srcName;
    }

    public String getSrcUnits() {
        return srcUnits;
    }

    public void setSrcUnits(String srcUnits) {
        this.srcUnits = srcUnits;
    }

    public String getLocalizedSrcName() {
        return localizedSrcName;
    }

    public void setLocalizedSrcName(String localizedSrcName) {
        this.localizedSrcName = localizedSrcName;
    }

    public String getLocalizedSrcUnits() {
        return localizedSrcUnits;
    }

    public void setLocalizedSrcUnits(String localizedSrcUnits) {
        this.localizedSrcUnits = localizedSrcUnits;
    }

    public String getRecommendedFormat() {
        return recommendedFormat;
    }

    public void setRecommendedFormat(String recommendedFormat) {
        this.recommendedFormat = recommendedFormat;
    }

    public int getData() {
        return data;
    }

    public void setData(int data) {
        this.data = data;
    }

    public int getMinLimit() {
        return minLimit;
    }

    public void setMinLimit(int minLimit) {
        this.minLimit = minLimit;
    }

    public int getMaxLimit() {
        return maxLimit;
    }

    public void setMaxLimit(int maxLimit) {
        this.maxLimit = maxLimit;
    }

    public String getFlags() {
        return flags;
    }

    public void setFlags(String flags) {
        this.flags = flags;
    }

    public int getGpu() {
        return gpu;
    }

    public void setGpu(int gpu) {
        this.gpu = gpu;
    }

    public int getSrcId() {
        return srcId;
    }

    public void setSrcId(int srcId) {
        this.srcId = srcId;
    }

}
