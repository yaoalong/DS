package lab.mars.ds.network.intialize;

import io.netty.channel.ChannelPipeline;
import lab.mars.ds.network.handler.RegisterPacketClientChannelHandler;
import lab.mars.ds.network.initializer.TcpChannelInitializer;

/**
 *
 */
public class RegisterPacketClientChannelInitializer extends
        TcpChannelInitializer {

    @Override
    public void init(ChannelPipeline ch) {
        ch.addLast(new RegisterPacketClientChannelHandler());
    }
}
