package lab.mars.ds.collaboration;

import lab.mars.ds.loadbalance.LoadBalanceException;
import lab.mars.ds.loadbalance.LoadBalanceService;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/*
 * 监控zookeeper,从而可以获取在线机器列表
 */
public class ZooKeeper_Monitor extends Thread implements Watcher {

    private static final Logger LOG = LoggerFactory
            .getLogger(ZooKeeper_Monitor.class);
    private static final String ROOT_NODE = "/server";
    private static CountDownLatch countDownLatch = new CountDownLatch(1);
    private ZooKeeper zooKeeper;
    private LoadBalanceService loadBalanceService;
    public ZooKeeper_Monitor(ZooKeeper zooKeeper){
        this.zooKeeper=zooKeeper;
    }
    public void run() {
        try {
            getChildrens();
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("zookeepeer_monitor is error because of:",
                    e);
        }
    }

    @Override
    public void process(WatchedEvent event) {
        if (EventType.NodeChildrenChanged == event.getType()
                && event.getPath().startsWith(ROOT_NODE)) {
            try {
                if (zooKeeper == null) {
                    return;
                }
                try {
                    getChildrens();
                } catch (LoadBalanceException e) {
                    e.printStackTrace();
                }
            } catch (KeeperException | InterruptedException e) {
                LOG.error("error:{}", e.getMessage());
            }
        }
    }

    /*
     * 去修改networkPool的服务器列表
     */
    private void getChildrens() throws KeeperException, InterruptedException, LoadBalanceException {
        if (zooKeeper == null) {
            LOG.error("zookeeper is empty");
            return;
        }
        List<String> servers = zooKeeper.getChildren(ROOT_NODE, this);
        loadBalanceService.setServers(servers);
    }


    public void setLoadBalanceService(LoadBalanceService loadBalanceService) {
        this.loadBalanceService = loadBalanceService;
    }
}
