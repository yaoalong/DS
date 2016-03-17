package lab.mars.ds.zookeeper.test;

import lab.mars.ds.loadbalance.LoadBalanceException;
import lab.mars.ds.loadbalance.impl.NetworkPool;
import lab.mars.ds.register.ZooKeeperRegister;

public class ZooKeeperRegisterTest {

    public static void main(String args[]) throws LoadBalanceException {
        NetworkPool networkPool = new NetworkPool();
        ZooKeeperRegister zooKeeperRegister = new ZooKeeperRegister();
        zooKeeperRegister.starter(args, networkPool);
        zooKeeperRegister.register("192.168.10.131" + ":" + 211);
    }
}
