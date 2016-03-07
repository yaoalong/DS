package lab.mars.ds.register.model;

import java.io.Serializable;

public class RegisterM2mPacket implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1142288182916269801L;

    private Integer type;

    private Integer body;

    public RegisterM2mPacket() {

    }

    public RegisterM2mPacket(Integer type, Integer body) {
        this.type = type;
        this.body = body;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getBody() {
        return body;
    }

    public void setBody(Integer body) {
        this.body = body;
    }

    public boolean isFinished() {
        // TODO Auto-generated method stub
        return false;
    }

}
