package lab.mars.ds.web.network;

import lab.mars.ds.web.network.initializer.WebClientChannelInitializer;
import lab.mars.ds.network.TcpClientNetwork;

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
            } finally {
                reentrantLock.unlock();
            }
        }
        channel.writeAndFlush(msg);
    }
}
