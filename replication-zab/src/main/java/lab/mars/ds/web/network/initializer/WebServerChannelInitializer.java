package lab.mars.ds.web.network.initializer;

import lab.mars.ds.web.network.handler.WebServerChannelHandler;
import io.netty.channel.ChannelPipeline;
import lab.mars.ds.loadbalance.LoadBalanceService;
import lab.mars.ds.network.initializer.TcpChannelInitializer;
import org.lab.mars.onem2m.server.NettyServerCnxn;
import org.lab.mars.onem2m.server.NettyServerCnxnFactory;

/**
 * 
 * @author yaoalong
 * @Date 2016年1月24日
 * @Email yaoalong@foxmail.com
 */
public class WebServerChannelInitializer extends TcpChannelInitializer {
    private NettyServerCnxnFactory nettyServerCnxnFactory;

    public WebServerChannelInitializer( NettyServerCnxnFactory nettyServerCnxnFactory) {
        this.nettyServerCnxnFactory = nettyServerCnxnFactory;
    }

    @Override
    public void init(ChannelPipeline ch) {
        ch.addLast(new WebServerChannelHandler(nettyServerCnxnFactory));

    }
}