package lab.mars.ds.network.initializer;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

public class TcpChannelInitializer extends ChannelInitializer<SocketChannel> {
    private SimpleChannelInboundHandler<Object> simpleChannelInboundHandler;

    public TcpChannelInitializer(
            SimpleChannelInboundHandler<Object> simpleChannelInboundHandler) {
        this.simpleChannelInboundHandler = simpleChannelInboundHandler;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline channelPipeline = ch.pipeline();
        channelPipeline.addLast(new ObjectEncoder());
        channelPipeline.addLast(new ObjectDecoder(ClassResolvers
                .cacheDisabled(null)));
        channelPipeline.addLast(simpleChannelInboundHandler);
    }

}