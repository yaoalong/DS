package lab.mars.ds.collaboration;

import org.apache.zookeeper.*;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;

public class RegisterIntoZooKeeper extends Thread  {

    private static final Logger LOG = LoggerFactory
            .getLogger(RegisterIntoZooKeeper.class);
    private ZooKeeper zooKeeper;
    private String value;
    private static final String ROOT="/server/";
    public void register(ZooKeeper zooKeeper,String value) throws IOException, KeeperException,
            InterruptedException {
        this.zooKeeper=zooKeeper;
        this.value = value;

    }

    @Override
    public void run() {
        try {
            zooKeeper.create(ROOT + value, "1".getBytes(Charset.forName("utf-8")),
                    Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        } catch (KeeperException | InterruptedException e) {
            if(LOG.isTraceEnabled()){
                LOG.trace("error because of:", e);
            }
        }
    }
}
