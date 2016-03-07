package lab.mars.ds.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.HashSet;
import java.util.Set;

import lab.mars.ds.network.intialize.PacketServerChannelInitializer;

import org.lab.mars.onem2m.server.ServerCnxnFactory;
import org.lab.mars.onem2m.server.quorum.M2mHandler;

/*
 * TCP服务器
 */
public class ZABTcpServer {
    private Set<Channel> channels;
    private ServerCnxnFactory serverCnxnFactory;
    private M2mHandler m2mHandler;

    public ZABTcpServer(ServerCnxnFactory serverCnxnFactory,
            M2mHandler m2mHandler) {
        this.serverCnxnFactory = serverCnxnFactory;
        this.channels = new HashSet<>();
        this.m2mHandler = m2mHandler;

    }

    public void bind(String host, int port) throws InterruptedException {
        ServerBootstrap b = new ServerBootstrap();
        b.group(NetworkEventLoopGroup.bossGroup,
                NetworkEventLoopGroup.workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_BACKLOG, 1000)
                .childHandler(
                        new PacketServerChannelInitializer(serverCnxnFactory,
                                m2mHandler));
        b.bind(host, port).addListener((ChannelFuture channelFuture) -> {
            channels.add(channelFuture.channel());
        });
    }

    public void close() {
        channels.forEach(channel -> channel.close());

    }

}
