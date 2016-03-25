package lab.mars.ds.web.network.initializer;

import io.netty.channel.ChannelPipeline;
import lab.mars.ds.network.initializer.TcpChannelInitializer;
import lab.mars.ds.web.network.handler.WebClientChannelHandler;

/**
 * @author yaoalong
 * @Date 2016年1月24日
 * @Email yaoalong@foxmail.com
 */
public class WebClientChannelInitializer extends TcpChannelInitializer {

    private Integer replicationFactor;

    public WebClientChannelInitializer(Integer replicationFactor) {
        this.replicationFactor = replicationFactor;
    }

    @Override
    public void init(ChannelPipeline ch) {
        ch.addLast(new WebClientChannelHandler(replicationFactor));

    }
}