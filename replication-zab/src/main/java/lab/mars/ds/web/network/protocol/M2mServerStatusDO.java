package lab.mars.ds.web.network.protocol;

/**
 * 
 * @author yaoalong
 * @Date 2016年1月24日
 * @Email yaoalong@foxmail.com
 */
/*
 * 用来向web展示的DO
 */
public class M2mServerStatusDO {

    private long id;

    private String ip;

    private Integer status;

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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

}
