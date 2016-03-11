package lab.mars.ds.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Author:yaoalong. Date:2016/3/3. Email:yaoalong@foxmail.com
 */
public abstract class TcpClientNetwork {

    private Channel channel;
    protected ReentrantLock reentrantLock = new ReentrantLock();
    protected Condition condition = reentrantLock.newCondition();
    private ChannelInitializer<SocketChannel> socketChannelChannelInitializer;

    public void connectionOne(String host, int port) {

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(NetworkEventLoopGroup.workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(socketChannelChannelInitializer);
        bootstrap.connect(host, port).addListener((ChannelFuture future) -> {
            reentrantLock.lock();
            channel = future.channel();
            condition.signalAll();
            reentrantLock.unlock();
        });

    }

    public abstract void write(Object msg);

    public void close() {
        if (channel != null) {
            channel.close();
        }
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public void setSocketChannelChannelInitializer(
            ChannelInitializer<SocketChannel> socketChannelChannelInitializer) {
        this.socketChannelChannelInitializer = socketChannelChannelInitializer;
    }
}