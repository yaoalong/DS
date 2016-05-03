package lab.mars.ds.web.network.initializer;

import lab.mars.ds.web.network.handler.WebServerChannelHandler;
import io.netty.channel.ChannelPipeline;
import lab.mars.ds.loadbalance.NetworkInterface;
import lab.mars.ds.network.initializer.TcpChannelInitializer;

/**
 * 
 * @author yaoalong
 * @Date 2016年1月24日
 * @Email yaoalong@foxmail.com
 */
public class WebServerChannelInitializer extends TcpChannelInitializer {
    private NetworkInterface networkInterface;

    public WebServerChannelInitializer(NetworkInterface networkInterface) {
        this.networkInterface = networkInterface;
    }

    @Override
    public void init(ChannelPipeline ch) {
        ch.addLast(new WebServerChannelHandler(networkInterface));

    }
}