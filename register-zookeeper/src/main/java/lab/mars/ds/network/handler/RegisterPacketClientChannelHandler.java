package lab.mars.ds.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lab.mars.ds.register.model.RegisterM2mPacket;
import lab.mars.ds.register.starter.Starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterPacketClientChannelHandler extends
        SimpleChannelInboundHandler<Object> {

    private static final Logger LOG = LoggerFactory
            .getLogger(RegisterPacketClientChannelHandler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        RegisterM2mPacket m2mPacket = (RegisterM2mPacket) msg;
        if (m2mPacket.getType() == 0) {
            synchronized (Starter.servers) {
                LOG.info("receive message:{}", m2mPacket.getBody());
                Starter.servers.add(m2mPacket.getBody());
            }

        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.info("close ctx,because of:{}", cause);
        ctx.close();
    }
}
