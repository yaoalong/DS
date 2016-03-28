package org.lab.mars.onem2m.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.net.InetSocketAddress;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import lab.mars.ds.loadbalance.LoadBalanceException;
import lab.mars.ds.loadbalance.impl.NetworkPool;

import org.lab.mars.onem2m.M2mKeeperException.Code;
import org.lab.mars.onem2m.proto.M2mPacket;
import org.lab.mars.onem2m.proto.M2mReplyHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyServerCnxn extends ServerCnxn {

    Logger LOG = LoggerFactory.getLogger(NettyServerCnxn.class);
    Channel channel;

    int sessionTimeout;
    ServerCnxnFactory factory;
    boolean initialized;
    /**
     * The ZooKeeperServer for this connection. May be null if the server is not
     * currently serving requests (for example if the server is not an active
     * quorum participant.
     */

    private ConcurrentHashMap<String, ZooKeeperServer> zookeeperServers;
    /**
     * 一致性hash环
     */
    private NetworkPool networkPool;

    public NettyServerCnxn(Channel channel,
            ConcurrentHashMap<String, ZooKeeperServer> zooKeeperServers,
            ServerCnxnFactory serverCnxnFactory) {
        this.channel = channel;
        this.zookeeperServers = zooKeeperServers;
        this.factory = serverCnxnFactory;

    }

    @Override
    public void close() {

        synchronized (factory.cnxns) {
            // if this is not in cnxns then it's already closed
            if (!factory.cnxns.remove(this)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("cnxns size:" + factory.cnxns.size());
                }
                return;
            }
        }

        if (channel.isOpen()) {
            channel.close();
        }
    }

    @Override
    public long getSessionId() {
        return 0L;
    }

    @Override
    public void setSessionId(long sessionId) {
    }

    @Override
    public int getSessionTimeout() {
        return sessionTimeout;
    }

    @Override
    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    /**
     * 应该在这里进行判断在哪个ZkServer进行处理
     *
     * @param ctx
     * @param m2mPacket
     * @throws LoadBalanceException
     */
    public void receiveMessage(ChannelHandlerContext ctx, M2mPacket m2mPacket)
            throws LoadBalanceException {
        String server = networkPool.getServer(m2mPacket.getM2mRequestHeader()
                .getKey());
        for (Entry<String, ZooKeeperServer> entry : zookeeperServers.entrySet()) {
            System.out.println("逐渐:" + entry.getKey());
        }
        if (zookeeperServers.get(server) == null) {
            for (int i = 0; i < 5; i++) {// 重新尝试五次
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
       factory.addPacketCount();
        if (zookeeperServers.get(server) == null) {
            M2mReplyHeader m2mReplyHeader = new M2mReplyHeader(0, 0L,
                    Code.SERVICE_IS_NOT_INIT.getCode());
            M2mPacket result = new M2mPacket(m2mPacket.getM2mRequestHeader(),
                    m2mReplyHeader, null, null);
            ctx.writeAndFlush(result);
        } else {
            zookeeperServers.get(server).processPacket(ctx, m2mPacket);
        }

    }

    @Override
    public long getOutstandingRequests() {
        return 0L;
    }

    @Override
    public InetSocketAddress getRemoteSocketAddress() {
        return null;
    }

    @Override
    protected ServerStats serverStats() {
        return null;
    }

    public void setNetworkPool(NetworkPool networkPool) {
        this.networkPool = networkPool;
    }

}
