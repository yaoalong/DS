package lab.mars.ds.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lab.mars.ds.register.model.RegisterM2mPacket;
import lab.mars.ds.register.starter.Starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterPacketServerChannelHandler extends
        SimpleChannelInboundHandler<Object> {
    private static Logger LOG = LoggerFactory
            .getLogger(RegisterPacketServerChannelHandler.class);
    private Starter register;

    public RegisterPacketServerChannelHandler(Starter register) {
        this.register = register;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        RegisterM2mPacket m2mPacket = (RegisterM2mPacket) msg;
        System.out.println("接收到了消息");
        if (m2mPacket.getType() == 0) {
            m2mPacket.setBody(1);
            ctx.writeAndFlush(m2mPacket);
        } else {
            register.start();
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        ctx.fireChannelRegistered();

    }

    ;

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

        ctx.close();
    }

    /*
     * 将server拆分为ip以及port
     */
    private String[] spilitString(String ip) {
        String[] splitMessage = ip.split(":");
        return splitMessage;
    }

}
