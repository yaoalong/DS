package lab.mars.ds.network.intialize;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import lab.mars.ds.register.starter.Starter;
import lab.mars.ds.network.handler.RegisterPacketServerChannelHandler;

public class RegisterPacketServerChannelInitializer extends
        ChannelInitializer<SocketChannel> {
    private Starter register;

    public RegisterPacketServerChannelInitializer(Starter register) {
        this.register = register;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline channelPipeline = ch.pipeline();
        channelPipeline.addLast(new ObjectEncoder());
        channelPipeline.addLast(new ObjectDecoder(ClassResolvers
                .cacheDisabled(null)));
        channelPipeline.addLast(new RegisterPacketServerChannelHandler(register));
    }
}