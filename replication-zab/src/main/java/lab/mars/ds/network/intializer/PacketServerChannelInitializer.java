package lab.mars.ds.network.intializer;

import io.netty.channel.ChannelPipeline;
import lab.mars.ds.connectmanage.LRUManage;
import lab.mars.ds.network.handler.PacketServerChannelHandler;
import lab.mars.ds.network.initializer.TcpChannelInitializer;

import org.lab.mars.onem2m.server.ServerCnxnFactory;
import org.lab.mars.onem2m.server.quorum.M2mHandler;

public class PacketServerChannelInitializer extends TcpChannelInitializer {
    private ServerCnxnFactory serverCnxnFactory;
    private M2mHandler m2mHandler;
    private LRUManage lruManage;

    public PacketServerChannelInitializer(ServerCnxnFactory serverCnxnFactory,
            M2mHandler m2mHandler, LRUManage lrumanage) {
        this.serverCnxnFactory = serverCnxnFactory;
        this.m2mHandler = m2mHandler;
        this.lruManage = lrumanage;
    }

    @Override
    public void init(ChannelPipeline ch) {
        ch.addLast(new PacketServerChannelHandler(serverCnxnFactory,
                m2mHandler, lruManage));

    }
}