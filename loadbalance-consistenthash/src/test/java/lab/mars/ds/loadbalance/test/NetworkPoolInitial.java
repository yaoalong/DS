package lab.mars.ds.loadbalance.test;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import lab.mars.ds.loadbalance.LoadBalanceException;
import lab.mars.ds.loadbalance.impl.NetworkPool;

import org.junit.Test;

/**
 * Author:yaoalong. Date:2016/3/7. Email:yaoalong@foxmail.com
 */
@SuppressWarnings("deprecation")
public class NetworkPoolInitial {
    @Test
    public void testInitial() throws LoadBalanceException {

        NetworkPool networkPool = new NetworkPool();
        List<String> servers = new ArrayList<>();
        for (Integer i = 0; i < 3; i++) {
            servers.add("192.168.10.131" + (2181 + i));
        }
        networkPool.setServers(servers);
        networkPool.initialize();
        Assert.assertEquals(Integer.valueOf(3), networkPool.getServerSize());

    }

    @Test
    public void testSetFactorAndInitial() throws LoadBalanceException {
        NetworkPool networkPool = new NetworkPool();
        List<String> servers = new ArrayList<String>();
        for (Integer i = 0; i < 3; i++) {
            servers.add("192.168.10.131" + (2181 + i));
        }
        networkPool.setServers(servers);
        networkPool.setNumOfVirtualNode(100);
        networkPool.initialize();
        Assert.assertEquals(Integer.valueOf(300), networkPool.getServerSize());
    }
}
