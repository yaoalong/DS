package lab.mars.ds.monitor;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import lab.mars.ds.loadbalance.NetworkInterface;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * 监控zookeeper,从而可以获取在线机器列表
 */
public class ZooKeeper_Monitor extends Thread implements Watcher {

    private static final Logger LOG = LoggerFactory
            .getLogger(ZooKeeper_Monitor.class);
    private static final String ROOT_NODE = "/server";
    private static CountDownLatch countDownLatch = new CountDownLatch(1);
    private ZooKeeper zooKeeper;
    /*
     * zooKeeper服务器的地址
     */
    private String server;
    private NetworkInterface networkPool;

    public void run() {
        try {
            zooKeeper = new ZooKeeper(server, 5000, this);
            countDownLatch.await();
            getChildrens();
            while (true) {
                zooKeeper.getChildren(ROOT_NODE, this);
                Thread.sleep(1000);
            }

        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("zookeepeer_monitor is error because of:{}",
                    e.getMessage());
        }
    }

    @Override
    public void process(WatchedEvent event) {
        if (KeeperState.SyncConnected == event.getState()
                && EventType.NodeChildrenChanged != event.getType()) {
            countDownLatch.countDown();
        } else if (EventType.NodeChildrenChanged == event.getType()
                && event.getPath().startsWith(ROOT_NODE)) {
            try {
                if (zooKeeper == null) {
                    return;
                }
                getChildrens();
            } catch (KeeperException | InterruptedException e) {
                LOG.error("error:{}", e.getMessage());
            }
        }
    }

    /*
     * 去修改networkPool的服务器列表
     */
    private void getChildrens() throws KeeperException, InterruptedException {
        if (zooKeeper == null) {
            LOG.error("zookeeper is empty");
            return;
        }
        List<String> serverStrings = zooKeeper.getChildren(ROOT_NODE, null);
        networkPool.setServers(serverStrings);
        networkPool.initialize();

    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public void setNetworkPool(NetworkInterface networkPool) {
        this.networkPool = networkPool;
    }
}
