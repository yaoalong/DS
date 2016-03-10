package lab.mars.ds.network.intializer;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import lab.mars.ds.connectmanage.LRUManage;
import lab.mars.ds.network.handler.PacketServerChannelHandler;

import org.lab.mars.onem2m.server.ServerCnxnFactory;
import org.lab.mars.onem2m.server.quorum.M2mHandler;

public class PacketServerChannelInitializer extends
        ChannelInitializer<SocketChannel> {
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
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline channelPipeline = ch.pipeline();
        channelPipeline.addLast(new ObjectEncoder());
        channelPipeline.addLast(new ObjectDecoder(ClassResolvers
                .cacheDisabled(null)));
        channelPipeline.addLast(new PacketServerChannelHandler(
                serverCnxnFactory, m2mHandler, lruManage));
    }
}