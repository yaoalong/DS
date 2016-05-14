package lab.mars.ds.collaboration;

import lab.mars.ds.loadbalance.LoadBalanceException;
import lab.mars.ds.loadbalance.LoadBalanceService;
import org.I0Itec.zkclient.ZkClient;
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
public class ZooKeeper_Monitor extends Thread {

    private static final Logger LOG = LoggerFactory
            .getLogger(ZooKeeper_Monitor.class);
    private static final String ROOT_NODE = "/server";
    private ZkClient zkClient;
    private LoadBalanceService loadBalanceService;
    private ZKRegisterAndMonitorService registerAndMonitorService;
    public ZooKeeper_Monitor(ZkClient zkClient,ZKRegisterAndMonitorService registerAndMonitorService){
        this.zkClient=zkClient;
        this.registerAndMonitorService=registerAndMonitorService;
    }
    @Override
    public void run() {
        try {
            loadBalanceService.setServers(zkClient.getChildren(ROOT_NODE));
        } catch (LoadBalanceException e) {
            e.printStackTrace();
        }
        synchronized (registerAndMonitorService.isOk){
            registerAndMonitorService.isOk.notify();
        }

        try {
            zkClient.subscribeChildChanges(ROOT_NODE,(path,childList)->{
                loadBalanceService.setServers(zkClient.getChildren(ROOT_NODE));
            });
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("zookeepeer_monitor is error because of:",
                    e);
        }
    }



    protected void setLoadBalanceService(LoadBalanceService loadBalanceService) {
        this.loadBalanceService = loadBalanceService;
    }
}
