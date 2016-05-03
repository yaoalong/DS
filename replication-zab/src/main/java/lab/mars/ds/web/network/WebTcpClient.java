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
                e.printStackTrace();
            } finally {
                reentrantLock.unlock();
            }
        }
        System.out.println("开始i"+port);
        channel.writeAndFlush(msg);
        System.out.println("结束"+port);
    }
}
