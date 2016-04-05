package lab.mars.ds.register;

import lab.mars.ds.loadbalance.LoadBalanceException;
import lab.mars.ds.loadbalance.NetworkInterface;

/**
 * Author:yaoalong. Date:2016/3/3. Email:yaoalong@foxmail.com
 */
public interface RegisterAndMonitorService {

    void register(String zooKeeperServer, String value, NetworkInterface networkInterface);
}
