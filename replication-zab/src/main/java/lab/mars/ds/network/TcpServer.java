package lab.mars.ds.network;

import lab.mars.ds.network.handler.PacketServerChannelHandler;
import lab.mars.ds.network.initializer.TcpChannelInitializer;

import org.lab.mars.onem2m.server.ServerCnxnFactory;
import org.lab.mars.onem2m.server.quorum.M2mHandler;

/*
 * TCP服务器
 */
public class TcpServer extends TcpServerNetwork {
    public TcpServer(ServerCnxnFactory serverCnxnFactory, M2mHandler m2mHandler) {
        super();
        PacketServerChannelHandler packetServerChannelHandler = new PacketServerChannelHandler(
                serverCnxnFactory, m2mHandler);
        setChannelChannelInitializer(new TcpChannelInitializer(
                packetServerChannelHandler));

    }

}
