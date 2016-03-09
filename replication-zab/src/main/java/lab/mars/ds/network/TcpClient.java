package lab.mars.ds.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import lab.mars.ds.network.intializer.PacketClientChannelInitializer;

import org.lab.mars.onem2m.proto.M2mPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TCP客户端
 */
public class TcpClient {

    private static final Logger LOG = LoggerFactory.getLogger(TcpClient.class);
    private Channel channel;

    private LinkedList<M2mPacket> pendingQueue;
    private ReentrantLock reentrantLock = new ReentrantLock();
    private Condition condition = reentrantLock.newCondition();

    public TcpClient() {

    }

    public TcpClient(LinkedList<M2mPacket> m2mPacket) {
        this.pendingQueue = m2mPacket;
    }

    public void connectionOne(String host, int port) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(NetworkEventLoopGroup.workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new PacketClientChannelInitializer(this));
        bootstrap.connect(host, port).addListener((ChannelFuture future) -> {
            reentrantLock.lock();
            channel = future.channel();
            condition.signalAll();
            reentrantLock.unlock();
        });

    }

    public void write(Object msg) {
        while (channel == null) {
            try {
                reentrantLock.lock();
                condition.await();
            } catch (InterruptedException e) {
                LOG.info("write error:{}", e);
                e.printStackTrace();
            } finally {
                reentrantLock.unlock();
            }
        }
        if (pendingQueue != null) {
            synchronized (pendingQueue) {
                pendingQueue.add((M2mPacket) msg);
            }

        }
        channel.writeAndFlush(msg);
        synchronized (msg) {
            while (!((M2mPacket) msg).isFinished()) {
                try {
                    ((M2mPacket) msg).wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("正式完成");
        }

    }

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

    public LinkedList<M2mPacket> getPendingQueue() {
        return pendingQueue;
    }

    public void setPendingQueue(LinkedList<M2mPacket> pendingQueue) {
        this.pendingQueue = pendingQueue;
    }

}
