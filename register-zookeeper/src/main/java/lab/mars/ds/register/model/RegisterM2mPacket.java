package lab.mars.ds.register.model;

import java.io.Serializable;

public class RegisterM2mPacket implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1142288182916269801L;

    private Integer type;

    private String body;

    public RegisterM2mPacket(Integer type, String body) {
        this.type = type;
        this.body = body;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

}
