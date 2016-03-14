package lab.mars.ds.network;

import java.util.LinkedList;

import lab.mars.ds.network.intializer.PacketClientChannelInitializer;

import org.lab.mars.onem2m.proto.M2mPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TCP客户端
 */
public class TcpClient extends TcpClientNetwork {

    private static final Logger LOG = LoggerFactory.getLogger(TcpClient.class);

    private LinkedList<M2mPacket> pendingQueue;

    public TcpClient() {
        setSocketChannelChannelInitializer(new PacketClientChannelInitializer(
                this));
    }

    public TcpClient(LinkedList<M2mPacket> m2mPacket) {
        this();
        this.pendingQueue = m2mPacket;

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
                    msg.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
        return;
    }

    public LinkedList<M2mPacket> getPendingQueue() {
        return pendingQueue;
    }

    public void setPendingQueue(LinkedList<M2mPacket> pendingQueue) {
        this.pendingQueue = pendingQueue;
    }

}
