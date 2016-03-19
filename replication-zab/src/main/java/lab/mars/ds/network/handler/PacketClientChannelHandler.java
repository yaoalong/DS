package lab.mars.ds.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.IOException;

import lab.mars.ds.network.TcpClient;

import org.lab.mars.onem2m.proto.M2mPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketClientChannelHandler extends
        SimpleChannelInboundHandler<Object> {

    private static final Logger LOG = LoggerFactory
            .getLogger(RegisterPacketClientChannelHandler.class);
    private TcpClient tcpClient;

    public PacketClientChannelHandler(TcpClient tcpClient) {
        this.tcpClient = tcpClient;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {

    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        try {
            readResponse((M2mPacket) msg);
        } catch (IOException e) {
            LOG.error("channel read error:{}", e);
        }
    }

    private void readResponse(M2mPacket m2mPacket) throws IOException {
        M2mPacket packet;
        synchronized (tcpClient.getPendingQueue()) {
            if (tcpClient.getPendingQueue().size() == 0) {
                throw new IOException("Nothing in the queue, but got "
                        + m2mPacket.getM2mReplyHeader().getXid());
            }
            packet = tcpClient.getPendingQueue().remove();
            packet.setFinished(true);
            synchronized (packet) {
                packet.setM2mReplyHeader(m2mPacket.getM2mReplyHeader());
                packet.setResponse(m2mPacket.getResponse());
                packet.notifyAll();
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
