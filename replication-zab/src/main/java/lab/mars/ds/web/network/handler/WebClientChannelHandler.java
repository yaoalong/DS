package lab.mars.ds.web.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lab.mars.ds.web.network.constant.WebOperateType;
import lab.mars.ds.web.network.protocol.M2mServerLoadDO;
import lab.mars.ds.web.network.protocol.M2mWebPacket;
import lab.mars.ds.web.network.protocol.M2mWebRetriveKeyResponse;
import lab.mars.ds.web.network.protocol.M2mWebServerLoadResponse;
import org.lab.mars.onem2m.proto.M2mRequestHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 
 * @author yaoalong
 * @Date 2016年1月24日
 * @Email yaoalong@foxmail.com
 */

/**
 * 对于web模块的消息handler
 */
public class WebClientChannelHandler extends
        SimpleChannelInboundHandler<Object> {
    private static final Logger LOG = LoggerFactory
            .getLogger(WebClientChannelHandler.class);
    private Integer replicationFactor;

    public WebClientChannelHandler(Integer replicationFactor) {
        this.replicationFactor = replicationFactor;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        if (LOG.isInfoEnabled()) {
            String host = ((InetSocketAddress) ctx.channel().remoteAddress())
                    .getAddress().getHostAddress();
            int port = ((InetSocketAddress) ctx.channel().remoteAddress())
                    .getPort();
            LOG.info("host:{},port:{}", host, port);
        }
    }

    /**
     * 处理接收到的别的Server对Web请求的回复
     */
    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (msg == null) {
            LOG.error("error because of :nullPointer");
            return;
        }
        M2mWebPacket m2mPacket = (M2mWebPacket) msg;
        if(m2mPacket.getM2mRequestHeader().getType()== WebOperateType.lookServerLoad.getCode()){
            M2mWebServerLoadResponse m2mWebServerLoadResponse = (M2mWebServerLoadResponse) m2mPacket
                    .getResponse();
            M2mRequestHeader m2mRequestHeader = m2mPacket.getM2mRequestHeader();
            for (M2mServerLoadDO index: m2mWebServerLoadResponse.getM2mServerLoadDOs()) {
                WebServerChannelHandler.serverLoadAndCtxConcurrentHashMap
                        .get(m2mPacket.getM2mRequestHeader().getXid()).getM2mServerLoadDOs()
                        .add(index);

            }
            WebServerChannelHandler.serverLoadResult.put(m2mRequestHeader.getXid(),
                    WebServerChannelHandler.serverLoadResult.get(m2mRequestHeader
                            .getXid()) + 1);
            if (WebServerChannelHandler.serverResult.get(m2mRequestHeader.getXid()) >= 2) {
                M2mWebPacket m2mWebPacket = new M2mWebPacket(m2mRequestHeader,
                        m2mPacket.getM2mReplyHeader(), m2mPacket.getRequest(),
                        new M2mWebServerLoadResponse(
                                 WebServerChannelHandler.serverLoadAndCtxConcurrentHashMap.get(
                                        m2mPacket.getM2mRequestHeader().getXid())
                                        .getM2mServerLoadDOs()));
                WebServerChannelHandler.serverLoadAndCtxConcurrentHashMap.get(m2mRequestHeader.getXid())
                        .getCtx().writeAndFlush(m2mWebPacket);
                WebServerChannelHandler.serverLoadAndCtxConcurrentHashMap.remove(m2mRequestHeader.getXid());
                WebServerChannelHandler.serverResult.remove(m2mRequestHeader
                        .getXid());
            }
        }
        else{
            M2mWebRetriveKeyResponse m2mWebRetriveKeyResponse = (M2mWebRetriveKeyResponse) m2mPacket
                    .getResponse();
            M2mRequestHeader m2mRequestHeader = m2mPacket.getM2mRequestHeader();
            for (String server : m2mWebRetriveKeyResponse.getServers()) {
                WebServerChannelHandler.retriveServerAndCtxConcurrentHashMap
                        .get(m2mPacket.getM2mRequestHeader().getXid()).getServers()
                        .add(server);

            }
            WebServerChannelHandler.serverResult.put(m2mRequestHeader.getXid(),
                    WebServerChannelHandler.serverResult.get(m2mRequestHeader
                            .getXid()) + 1);
            if (WebServerChannelHandler.serverResult.get(m2mRequestHeader.getXid()) >= replicationFactor) {
                M2mWebPacket m2mWebPacket = new M2mWebPacket(m2mRequestHeader,
                        m2mPacket.getM2mReplyHeader(), m2mPacket.getRequest(),
                        new M2mWebRetriveKeyResponse(
                                (List<String>) WebServerChannelHandler.retriveServerAndCtxConcurrentHashMap.get(
                                        m2mPacket.getM2mRequestHeader().getXid())
                                        .getServers()));
                WebServerChannelHandler.retriveServerAndCtxConcurrentHashMap.get(m2mRequestHeader.getXid())
                        .getCtx().writeAndFlush(m2mWebPacket);
                WebServerChannelHandler.retriveServerAndCtxConcurrentHashMap.remove(m2mRequestHeader.getXid());
                WebServerChannelHandler.serverResult.remove(m2mRequestHeader
                        .getXid());
            }
        }

    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if (LOG.isInfoEnabled()) {
            LOG.info("ctx will be closed,because of :{}", cause);
        }
        ctx.close();
    }
}