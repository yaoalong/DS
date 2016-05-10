package lab.mars.ds.web.protocol;

import java.io.Serializable;

/**
 * 
 * @author yaoalong
 * @Date 2016年1月24日
 * @Email yaoalong@foxmail.com
 */
/*
 * 用来向web展示的DO
 */
public class M2mServerStatusDO  implements Serializable{

    private static final long serialVersionUID = 7929067295236794068L;
    private long id;

    private String ip;

    private int status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
