package lu.dainesch.homeinflux.input.msiafter.data;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "error")
@XmlAccessorType(XmlAccessType.FIELD)
public class AfterBurnerError implements Serializable {
    
    private String message;
    private String innerMessage;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getInnerMessage() {
        return innerMessage;
    }

    public void setInnerMessage(String innerMessage) {
        this.innerMessage = innerMessage;
    }
    
    
    
}
