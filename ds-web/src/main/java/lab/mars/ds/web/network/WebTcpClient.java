package lab.mars.ds.web.network;

import io.netty.channel.Channel;
import lab.mars.ds.network.TcpClientNetwork;
import lab.mars.ds.web.network.initializer.WebClientChannelInitializer;

public class WebTcpClient extends TcpClientNetwork {

    public WebTcpClient(Integer replicationFactor) {
        setSocketChannelChannelInitializer(new WebClientChannelInitializer(
                replicationFactor));
    }

    @Override
    public void write(Object msg) {
        while (channel == null) {
            try {
                reentrantLock.lock();
                condition.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                reentrantLock.unlock();
            }
        }
        channel.writeAndFlush(msg);
    }


}
