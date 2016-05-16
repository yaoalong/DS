package org.lab.mars.onem2m;

/**
 * Author:yaoalong.
 * Date:2016/5/16.
 * Email:yaoalong@foxmail.com
 */
public class IpAndPortDO {
    private String ip;
    private int port;


    public IpAndPortDO(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        return ip;
    }


    public int getPort() {
        return port;
    }


}
