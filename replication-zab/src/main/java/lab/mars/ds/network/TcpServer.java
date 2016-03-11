package lab.mars.ds.network;

import lab.mars.ds.connectmanage.LRUManage;
import lab.mars.ds.network.intializer.PacketServerChannelInitializer;

import org.lab.mars.onem2m.server.ServerCnxnFactory;
import org.lab.mars.onem2m.server.quorum.M2mHandler;

/*
 * TCP服务器
 */
public class TcpServer extends TcpServerNetwork {

    public TcpServer(ServerCnxnFactory serverCnxnFactory,
            M2mHandler m2mHandler, Integer numberOfConnections) {
        setChannelChannelInitializer(new PacketServerChannelInitializer(
                serverCnxnFactory, m2mHandler, new LRUManage(
                        numberOfConnections)));

    }

}
