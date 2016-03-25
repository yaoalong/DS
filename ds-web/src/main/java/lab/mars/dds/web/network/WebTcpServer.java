package lab.mars.dds.web.network;

import lab.mars.ds.loadbalance.NetworkInterface;
import lab.mars.ds.network.TcpServerNetwork;
import lab.mars.ds.web.network.initializer.WebServerChannelInitializer;

public class WebTcpServer extends TcpServerNetwork {
    public WebTcpServer(NetworkInterface networkInterface) {
        setChannelChannelInitializer(new WebServerChannelInitializer(
                networkInterface));
    }
}
