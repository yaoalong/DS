package lab.mars.ds.collaboration;

import lab.mars.ds.loadbalance.LoadBalanceService;
import lab.mars.ds.register.RegisterAndMonitorService;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * Author:yaoalong.
 * Date:2016/3/30.
 * Email:yaoalong@foxmail.com
 */

/**
 * 基于ZooKeeper实现的注册服务
 */
public class ZKRegisterAndMonitorService implements RegisterAndMonitorService {
    private static final Logger LOG = LoggerFactory
            .getLogger(ZKRegisterAndMonitorService.class);
    private static final String ROOT = "/server/";
    private ZkClient zkClient;

    protected  Boolean isOk=Boolean.FALSE;

    @Override
    public void register(String zooKeeperServer, String value, LoadBalanceService loadBalanceService) throws IOException {
        ZkClient zkClient = new ZkClient(zooKeeperServer, 5000);
        if(!zkClient.exists(ROOT+value)){
            zkClient.createEphemeral(ROOT + value);
        }
        else{
            zkClient.delete(ROOT+value);
            zkClient.createEphemeral(ROOT + value);
        }
        ZooKeeper_Monitor zooKeeper_monitor = new ZooKeeper_Monitor(zkClient,this);
        zooKeeper_monitor.setLoadBalanceService(loadBalanceService);
        zooKeeper_monitor.start();
        synchronized (isOk){

            try {
                isOk.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


        }

    }



    @Override
    public void close() {
        zkClient.close();
    }
}
