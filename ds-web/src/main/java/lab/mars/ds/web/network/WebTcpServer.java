package lab.mars.ds.web.network;

import lab.mars.ds.loadbalance.NetworkInterface;
import lab.mars.ds.network.TcpServerNetwork;
import lab.mars.ds.web.network.initializer.WebServerChannelInitializer;

public class WebTcpServer extends TcpServerNetwork {
    public WebTcpServer(NettyServerCnxnFactory nettyServerCnxnFactory) {
        setChannelChannelInitializer(new WebServerChannelInitializer(
                nettyServerCnxnFactory));
    }
}
