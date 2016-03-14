package lab.mars.ds.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lab.mars.ds.constant.OperateConstant;
import lab.mars.ds.register.model.RegisterM2mPacket;
import lab.mars.ds.register.starter.Starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterPacketServerChannelHandler extends
        SimpleChannelInboundHandler<Object> {
    private static Logger LOG = LoggerFactory
            .getLogger(RegisterPacketServerChannelHandler.class);
    private Starter register;

    private String myServer;

    public RegisterPacketServerChannelHandler(Starter register) {
        this.register = register;
        this.myServer = register.getMyServer();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        RegisterM2mPacket m2mPacket = (RegisterM2mPacket) msg;
        if (m2mPacket.getType() == OperateConstant.DETECT.getCode()) {
            m2mPacket.setBody(myServer);
            ctx.writeAndFlush(m2mPacket);
        } else {
            register.start();
        }
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
