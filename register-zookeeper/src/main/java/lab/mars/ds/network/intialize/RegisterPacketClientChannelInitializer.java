package lab.mars.ds.network.intialize;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

import lab.mars.ds.network.handler.RegisterPacketClientChannelHandler;

/**
 *
 */
public class RegisterPacketClientChannelInitializer extends
        ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(new RegisterPacketClientChannelHandler());
    }
}
