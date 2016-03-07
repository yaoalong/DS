package lab.mars.ds.network.intialize;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import lab.mars.ds.network.TcpClient;
import lab.mars.ds.network.handler.PacketClientChannelHandler;

import org.lab.mars.onem2m.server.util.PacketChannelIntializerOperate;

/**
 *
 */
public class PacketClientChannelInitializer extends
        ChannelInitializer<SocketChannel> {
    private TcpClient tcpClient;

    public PacketClientChannelInitializer(TcpClient tcpClient) {
        this.tcpClient = tcpClient;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        PacketChannelIntializerOperate.initChannel(ch);
        ch.pipeline().addLast(new PacketClientChannelHandler(tcpClient));
    }
}
