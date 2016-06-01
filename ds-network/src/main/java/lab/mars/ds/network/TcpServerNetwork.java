package lab.mars.ds.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.HashSet;
import java.util.Set;

/**
 * Author:yaoalong. Date:2016/3/3. Email:yaoalong@foxmail.com
 */
public class TcpServerNetwork {
    private Set<Channel> channels;
    private ChannelInitializer<SocketChannel> channelChannelInitializer;
    public void bind(String host, int port) throws InterruptedException {
        channels = new HashSet<>();
        ServerBootstrap b = new ServerBootstrap();
        b.group(NetworkEventLoopGroup.bossGroup,
                NetworkEventLoopGroup.workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_BACKLOG, 1000)
                .childHandler(channelChannelInitializer);
        b.bind(host, port).addListener((ChannelFuture channelFuture) -> {
            if(channelFuture.isSuccess()){
                System.out.println("成功"+port);
            }
            else{
                System.out.println("失败"+port);
            }
            channels.add(channelFuture.channel());
        });
    }

    public void close() {
        channels.forEach(channel -> channel.close());
    }

    public void setChannelChannelInitializer(
            ChannelInitializer<SocketChannel> channelChannelInitializer) {
        this.channelChannelInitializer = channelChannelInitializer;
    }
}
