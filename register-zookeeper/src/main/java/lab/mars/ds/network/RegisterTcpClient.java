package lab.mars.ds.network;

public class RegisterTcpClient extends TcpClientNetwork {

    public RegisterTcpClient() {
    }

    @Override
    public void write(Object msg) {
        while (getChannel() == null) {
            try {
                reentrantLock.lock();
                condition.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                reentrantLock.unlock();
            }
        }

        getChannel().writeAndFlush(msg);

    }

}
