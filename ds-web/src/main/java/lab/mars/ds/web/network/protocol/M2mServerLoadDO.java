package lab.mars.ds.web.network.protocol;

import java.io.Serializable;

public class M2mServerLoadDO implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 7514662116801082052L;
    private String ip;
    private Long count;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

}
