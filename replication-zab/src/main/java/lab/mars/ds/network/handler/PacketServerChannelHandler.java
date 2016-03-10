package lab.mars.ds.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import lab.mars.ds.connectmanage.LRUManage;
import lab.mars.ds.loadbalance.impl.NetworkPool;
import lab.mars.ds.network.TcpClient;

import org.lab.mars.onem2m.proto.M2mPacket;
import org.lab.mars.onem2m.server.NettyServerCnxn;
import org.lab.mars.onem2m.server.ServerCnxnFactory;
import org.lab.mars.onem2m.server.quorum.M2mHandler;
import org.lab.mars.onem2m.server.quorum.M2mHandlerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketServerChannelHandler extends
        SimpleChannelInboundHandler<Object> {
    private static final AttributeKey<NettyServerCnxn> STATE = AttributeKey
            .valueOf("PacketServerChannelHandler.nettyServerCnxn");
    private static Logger LOG = LoggerFactory
            .getLogger(RegisterPacketServerChannelHandler.class);
    private final LinkedList<M2mPacket> pendingQueue = new LinkedList<M2mPacket>();
    private ServerCnxnFactory serverCnxnFactory;
    private ConcurrentHashMap<String, TcpClient> ipAndTcpClient = new ConcurrentHashMap<>();
    private String self;
    private NetworkPool networkPool;
    private M2mHandler m2mHandler;

    private LRUManage lruManage = new LRUManage(16);// TODO 这个应该用户来定义

    public PacketServerChannelHandler(ServerCnxnFactory serverCnxnFactory,
            M2mHandler m2mHandler) {
        this.serverCnxnFactory = serverCnxnFactory;
        this.self = serverCnxnFactory.getMyIp();
        this.networkPool = serverCnxnFactory.getNetworkPool();
        this.m2mHandler = m2mHandler;

    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        System.out.println("收到消息");
        lruManage.refresh(ctx.channel());
        M2mPacket m2mPacket = (M2mPacket) msg;
        if (preProcessPacket(m2mPacket, ctx)) {
            if (m2mHandler == null) {
                NettyServerCnxn nettyServerCnxn = ctx.attr(STATE).get();
                nettyServerCnxn.receiveMessage(ctx, m2mPacket);
            } else {
                M2mHandlerResult m2mHandlerResult = m2mHandler.recv(m2mPacket);

                boolean isDistributed = m2mHandlerResult.isFlag();
                if (isDistributed == true) {
                    NettyServerCnxn nettyServerCnxn = ctx.attr(STATE).get();
                    nettyServerCnxn.receiveMessage(ctx,
                            m2mHandlerResult.getM2mPacket());
                }
            }

        } else {// 需要增加对错误的处理

        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        lruManage.add(ctx.channel());
        NettyServerCnxn nettyServerCnxn = new NettyServerCnxn(ctx.channel(),
                serverCnxnFactory.getZkServers(), serverCnxnFactory);
        nettyServerCnxn.setNetworkPool(serverCnxnFactory.getNetworkPool());
        ctx.attr(STATE).set(nettyServerCnxn);
        ctx.fireChannelRegistered();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.info("Channel disconnect caused close:{}", cause);
        cause.printStackTrace();
        ctx.close();
    }

    /**
     * 对数据包进行处理
     *
     * @param m2mPacket
     * @return
     */
    public boolean preProcessPacket(M2mPacket m2mPacket,
            ChannelHandlerContext ctx) {
        String key = m2mPacket.getM2mRequestHeader().getKey();

        String trueServer = networkPool.getTrueServer(key);
        if (trueServer.equals(self)) {
            return true;
        }
        String server = networkPool.getServer(key);
        List<String> responseServers = networkPool
                .getReplicationServer(trueServer);
        for (String responseServer : responseServers) {
            if (responseServer.equals(self)) {
                return true;
            }
            if (networkPool.getServers().contains(responseServer)) {
                if (ipAndTcpClient.containsKey(responseServer)) {
                    ipAndTcpClient.get(responseServer).write(m2mPacket);
                    return false;
                } else {
                    try {
                        TcpClient tcpClient = new TcpClient(pendingQueue);
                        String[] splitStrings = spilitString(responseServer);
                        tcpClient.connectionOne(splitStrings[0],
                                Integer.valueOf(splitStrings[1]));

                        tcpClient.write(m2mPacket);
                        ctx.writeAndFlush(m2mPacket);

                        ipAndTcpClient.put(server, tcpClient);
                        return false;
                    } catch (Exception e) {
                        LOG.error("process packet error:{}", e);
                    }
                }

            }
        }

        return false;
    }

    /*
     * 将server拆分为ip以及port
     */
    private String[] spilitString(String ip) {
        String[] splitMessage = ip.split(":");
        return splitMessage;
    }

}
