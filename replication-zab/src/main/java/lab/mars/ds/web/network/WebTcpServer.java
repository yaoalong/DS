package lab.mars.ds.web.network;

import lab.mars.ds.web.network.initializer.WebServerChannelInitializer;
import lab.mars.ds.network.TcpServerNetwork;
import org.lab.mars.onem2m.server.NettyServerCnxnFactory;

public class WebTcpServer extends TcpServerNetwork {
    public WebTcpServer(NettyServerCnxnFactory nettyServerCnxnFactory) {
        setChannelChannelInitializer(new WebServerChannelInitializer(
                nettyServerCnxnFactory));
    }
}
