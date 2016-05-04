package lab.mars.ds.web.network.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lab.mars.ds.loadbalance.NetworkInterface;
import lab.mars.ds.web.network.WebTcpClient;
import lab.mars.ds.web.network.constant.WebOperateType;
import lab.mars.ds.web.protocol.*;
import org.lab.mars.onem2m.server.NettyServerCnxnFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 对于web模块的server handler
 */
public class WebServerChannelHandler extends
        SimpleChannelInboundHandler<Object> {
    private static final ConcurrentHashMap<String, Channel> webAddressAndPortToChannel = new ConcurrentHashMap<String, Channel>();
    static ConcurrentHashMap<Integer, RetriveServerAndCtx> retriveServerAndCtxConcurrentHashMap = new ConcurrentHashMap<Integer, RetriveServerAndCtx>();
    static ConcurrentHashMap<Integer,ServerLoadAndCtx> serverLoadAndCtxConcurrentHashMap=new ConcurrentHashMap<>();
    static ConcurrentHashMap<Integer, Integer> serverResult = new ConcurrentHashMap<Integer, Integer>();

    static ConcurrentHashMap<Integer,Integer> serverLoadResult=new ConcurrentHashMap<>();
    private static Logger LOG = LoggerFactory
            .getLogger(WebServerChannelHandler.class);
    private NetworkInterface networkInterface;
    private AtomicInteger zxid = new AtomicInteger(0);
    private NettyServerCnxnFactory nettyServerCnxnFactory;
    public WebServerChannelHandler(NettyServerCnxnFactory nettyServerCnxnFactory) {
        this.nettyServerCnxnFactory=nettyServerCnxnFactory;
        this.networkInterface = nettyServerCnxnFactory.getNetworkPool();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        try {
            System.out.println("接收到了数据");
            M2mWebPacket m2mPacket = (M2mWebPacket) msg;
            int operateType = m2mPacket.getM2mRequestHeader().getType();
            if (operateType == WebOperateType.getStatus.getCode()) {
                try {
                    lookAllServerStatus(m2mPacket, ctx.channel());
                } catch (IOException e) {
                    if (LOG.isInfoEnabled()) {
                        LOG.error("channelRead is error:because of:{}",
                                e.getMessage());
                    }
                }
            } else if (operateType == WebOperateType.retriveLocalKey.getCode()) {
                System.out.println("xxxx");
                String key = m2mPacket.getM2mRequestHeader().getKey();
                List<String> servers = networkInterface
                        .getReplicationServers(networkInterface
                                .getTrueServer(key));

                List<String> resultList = servers.stream().distinct()
                        .collect(Collectors.toList());
                resultList.forEach(t -> {
                    System.out.println("result:" + t);
                });
                M2mWebPacket m2mWebPacket = new M2mWebPacket(
                        m2mPacket.getM2mRequestHeader(),
                        m2mPacket.getM2mReplyHeader(), m2mPacket.getRequest(),
                        new M2mWebRetriveKeyResponse(servers));
                ctx.writeAndFlush(m2mWebPacket);
            } else if (operateType == WebOperateType.lookRemoteServerLoad.getCode()) {
                System.out.println("开始处理远程serverLoad");
                m2mPacket.getM2mRequestHeader().setType(WebOperateType.lookServerLoad.getCode());
                int cid=zxid.getAndIncrement();
                m2mPacket.getM2mRequestHeader().setXid(cid);
                System.out.println("cid:"+cid+" channel:"+ctx.toString());
                serverLoadAndCtxConcurrentHashMap.put(cid,new ServerLoadAndCtx(ctx,new ArrayList<M2mServerLoadDO>()));
                serverLoadResult.put(cid,0);
                for(Map.Entry<Long,String> entry:nettyServerCnxnFactory.getWebServer().entrySet()){
                    WebTcpClient webTcpClient=new WebTcpClient(null);
                    System.out.println("fvx"+entry.getValue());
                    String[] value=spilitString(entry.getValue());
                    webTcpClient.connectionOne(value[0],Integer.valueOf(value[1]));
                    webTcpClient.write(m2mPacket);
                }
                System.out.println("GGG");
            }
            else if(operateType==WebOperateType.retriveRemoteKey.getCode()){
                m2mPacket.getM2mRequestHeader().setType(WebOperateType.retriveLocalKey.getCode());
                int cid=zxid.getAndIncrement();
                m2mPacket.getM2mRequestHeader().setXid(cid);
                System.out.println("cid:"+cid+" channel:"+ctx.toString());
                retriveServerAndCtxConcurrentHashMap.put(cid,new RetriveServerAndCtx(ctx,new HashSet<String>()));
                serverResult.put(cid,0);
                for(Map.Entry<Long,String> entry:nettyServerCnxnFactory.getWebServer().entrySet()){
                    WebTcpClient webTcpClient=new WebTcpClient(null);
                    System.out.println("fvx"+entry.getValue());
                    String[] value=spilitString(entry.getValue());
                    webTcpClient.connectionOne(value[0],Integer.valueOf(value[1]));
                    webTcpClient.write(m2mPacket);
                }
                System.out.println("GGG");
            }
            else if (operateType == WebOperateType.lookReplicationServers.getCode()) {
                String server = m2mPacket.getM2mRequestHeader().getKey();
                List<String> servers = networkInterface.getReplicationServer(server);
                List<M2mServerStatusDO> result = new ArrayList<>();
                servers.forEach(t -> {
                    M2mServerStatusDO m2mServerStatusDO = new M2mServerStatusDO();
                    m2mServerStatusDO.setIp(t);
                });
                M2mWebPacket m2mWebPacket = new M2mWebPacket(
                        m2mPacket.getM2mRequestHeader(),
                        m2mPacket.getM2mReplyHeader(), m2mPacket.getRequest(),
                        new M2mWebReplicationServersResponse(result));
                ctx.writeAndFlush(m2mWebPacket);
            }
            else if(operateType== WebOperateType.lookServerLoad.getCode()){
                    System.out.println("开始处理本地serverLoad");
                 List<M2mServerLoadDO> m2mServerLoadDOs=new ArrayList<>();
                    M2mServerLoadDO m2mServerLoadDO=new M2mServerLoadDO();
                 m2mServerLoadDO.setLabel(nettyServerCnxnFactory.getMyWebIp());
                m2mServerLoadDO.setY(nettyServerCnxnFactory.getPacketCount());
                m2mServerLoadDOs.add(m2mServerLoadDO);
                 M2mWebPacket m2mWebPacket = new M2mWebPacket(
                 m2mPacket.getM2mRequestHeader(),
                 m2mPacket.getM2mReplyHeader(), m2mPacket.getRequest(),
                 new M2mWebServerLoadResponse(m2mServerLoadDOs));
                ctx.writeAndFlush(m2mWebPacket);
            }

            // } else if (operateType ==
            // WebOperateType.retriveLocalKey.getCode()) { // 查看本地是否包含一个key
            // String key = m2mPacket.getM2mRequestHeader().getKey();
            // if (key == null) {
            // if (LOG.isInfoEnabled()) {
            // LOG.info("key is null");
            // }
            // }
            // final ConcurrentHashMap<String, ZooKeeperServer> zookeeperServers
            // = serverCnxnFactory
            // .getZkServers();
            // Set<String> servers = new HashSet<String>();
            // for (Map.Entry<String, ZooKeeperServer> entry : zookeeperServers
            // .entrySet()) {
            // ZooKeeperServer zooKeeperServer = entry.getValue();
            // ZKDatabase zkDatabase = zooKeeperServer.getZKDatabase();
            // if (zkDatabase.getM2mData().getNodes().containsKey(key)) {
            // servers.add(entry.getKey());
            // }
            // }
            // m2mPacket.getM2mRequestHeader().setType(
            // WebOperateType.ReplyRetriverRemoteKey.getCode());
            // M2mWebPacket m2mWebPacket = new M2mWebPacket(
            // m2mPacket.getM2mRequestHeader(),
            // m2mPacket.getM2mReplyHeader(), m2mPacket.getRequest(),
            // new M2mWebRetriveKeyResponse(servers));
            // ctx.writeAndFlush(m2mWebPacket);
            // } else if (operateType ==
            // WebOperateType.retriveRemoteKey.getCode()) {
            // System.out.println("进入到这里");
            // String server = networkPool.getSock(m2mPacket
            // .getM2mRequestHeader().getKey());
            // int zxid = getNextZxid();
            // result.put(zxid, new RetriveServerAndCtx(ctx,
            // new HashSet<String>()));
            // serverResult.put(zxid, 0);
            // m2mPacket.getM2mRequestHeader().setXid(zxid);
            // m2mPacket.getM2mRequestHeader().setType(
            // WebOperateType.retriveLocalKey.getCode());
            // Long position = networkPool.getServerPosition().get(server);
            // for (int i = 0; i < serverCnxnFactory.getReplicationFactor();
            // i++) {
            //
            // String address = spilitString(networkPool
            // .getPositionToServer().get(position + i))[0];
            // Integer webPort = NetworkPool.webPort.get(networkPool
            // .getPositionToServer().get(position + i));
            // WebTcpClient tcpClient = new WebTcpClient(
            // serverCnxnFactory.getReplicationFactor());
            // if (webAddressAndPortToChannel.containsKey(address
            // + webPort)) {
            // webAddressAndPortToChannel.get(address + webPort)
            // .writeAndFlush(m2mPacket);
            // } else {
            // tcpClient.connectionOne(address, webPort);
            // tcpClient.write(m2mPacket);
            // webAddressAndPortToChannel.put(address + webPort,
            // tcpClient.getChannel());
            //
            // }
            //
            // }
            //
            // }
            // else if(operateType==WebOperateType.lookServerLoad.getCode()){
            //
            // }
            // else
            // if(operateType==WebOperateType.lookRemoteServerLoad.getCode()){
            //
            // }
            // else {
            // if (LOG.isDebugEnabled()) {
            // LOG.info("invalid operate type : {}", m2mPacket
            // .getM2mRequestHeader().getType());
            // }
            // }
            // }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 去查询所有的server的状态
     *
     * @param m2mWebPacket
     * @param channel
     * @throws IOException
     */
    public void lookAllServerStatus(M2mWebPacket m2mWebPacket, Channel channel)
            throws IOException {
        M2mServerStatusDOs m2mServerStatuses = new M2mServerStatusDOs();
//        final TreeMap<Long, String> survivalServers = networkInterface
//                .getConsistentBuckets();
        List<M2mServerStatusDO> m2mServerStatusDOs = new ArrayList<>();
        List<String> survivalServers = networkInterface.getServers();
        List<String> allServers = networkInterface.getAllServers();
        survivalServers.forEach(t -> {
            M2mServerStatusDO m2mServerStatusDO = new M2mServerStatusDO();
            m2mServerStatusDO.setIp(t);
            m2mServerStatusDO.setStatus(M2mServerStatus.STARTED
                    .getStatus());
            m2mServerStatusDOs.add(m2mServerStatusDO);
        });
        allServers.forEach(t -> {
            if (!survivalServers.contains(t)){
                M2mServerStatusDO m2mServerStatusDO = new M2mServerStatusDO();
                m2mServerStatusDO.setIp(t);
                m2mServerStatusDO.setStatus(M2mServerStatus.STARTED
                        .getStatus());
                m2mServerStatusDOs.add(m2mServerStatusDO);
            }
        });

        m2mServerStatuses.setM2mServerStatusDOs(m2mServerStatusDOs);
        M2mWebServerStatusResponse m2mWebServerStatusResponse = new M2mWebServerStatusResponse();
        M2mWebPacket m2mPacket = M2mWebPacketHandle.createM2mWebPacket(
                m2mWebPacket.getM2mRequestHeader(),
                m2mWebPacket.getM2mReplyHeader(), m2mWebPacket.getRequest(),
                m2mWebServerStatusResponse, m2mServerStatuses,
                "m2mWebServerStatuses");
        channel.writeAndFlush(m2mPacket);

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        ctx.fireChannelRegistered();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Channel disconnect caused close:{}", cause);
        }
        ctx.close();
    }

    public int getNextZxid() {
        return zxid.getAndIncrement();
    }

    /*
     * 将server拆分为ip以及port
     */
    private String[] spilitString(String ip) {
        String[] splitMessage = ip.split(":");
        return splitMessage;
    }

}
