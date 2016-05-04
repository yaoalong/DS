package lab.mars.ds.register;

import lab.mars.ds.loadbalance.LoadBalanceService;

import java.io.IOException;

/**
 * Author:yaoalong. Date:2016/3/3. Email:yaoalong@foxmail.com
 */
public interface RegisterAndMonitorService {

    void register(String zooKeeperServer, String value, LoadBalanceService loadBalanceService) throws IOException;

    void close();
}
