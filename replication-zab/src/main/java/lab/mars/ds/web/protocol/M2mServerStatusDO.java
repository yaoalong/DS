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
    private Long id;

    private String ip;

    private Integer status;

    public Long getId() {
        if(id==null)return 0L;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
