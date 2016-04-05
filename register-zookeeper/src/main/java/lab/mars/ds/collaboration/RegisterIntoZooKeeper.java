package lab.mars.ds.collaboration;

import org.apache.zookeeper.*;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class RegisterIntoZooKeeper extends Thread implements Watcher {

    private static final Logger LOG = LoggerFactory
            .getLogger(RegisterIntoZooKeeper.class);
    private static CountDownLatch countDownLatch = new CountDownLatch(1);
    private String server;
    private ZooKeeper zooKeeper;
    private String value;

    public void register(String value) throws IOException, KeeperException,
            InterruptedException {
        zooKeeper = new ZooKeeper(server, 5000, new RegisterIntoZooKeeper());
        this.value = value;

    }

    @Override
    public void run() {
        try {
            countDownLatch.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            zooKeeper.create("/server/" + value, "1".getBytes(),
                    Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        } catch (KeeperException | InterruptedException e) {
            LOG.trace("error because of:{}", e);
        }
    }

    @Override
    public void process(WatchedEvent event) {
        System.out.println("Receive watched event:" + event);
        if (KeeperState.SyncConnected == event.getState()) {
            countDownLatch.countDown();
        }

    }
    public void setServer(String server) {
        this.server = server;
    }


}
