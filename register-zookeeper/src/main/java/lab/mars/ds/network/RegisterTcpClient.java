package lab.mars.ds.network;

import lab.mars.ds.network.intialize.RegisterPacketClientChannelInitializer;

public class RegisterTcpClient extends TcpClientNetwork {

    public RegisterTcpClient(){
        setSocketChannelChannelInitializer(new RegisterPacketClientChannelInitializer());
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
