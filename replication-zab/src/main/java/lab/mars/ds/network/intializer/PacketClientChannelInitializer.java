package lab.mars.ds.network.intializer;

import io.netty.channel.ChannelPipeline;
import lab.mars.ds.network.TcpClient;
import lab.mars.ds.network.handler.PacketClientChannelHandler;
import lab.mars.ds.network.initializer.TcpChannelInitializer;

/**
 *
 */
public class PacketClientChannelInitializer extends TcpChannelInitializer {
    private TcpClient tcpClient;

    public PacketClientChannelInitializer(TcpClient tcpClient) {
        this.tcpClient = tcpClient;
    }

    @Override
    public void init(ChannelPipeline ch) {

        ch.addLast(new PacketClientChannelHandler(tcpClient));

    }
}
