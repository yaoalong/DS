package lab.mars.ds.loadbalance.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lab.mars.ds.loadbalance.impl.NetworkPool;

import org.junit.Test;

/**
 * Author:yaoalong. Date:2016/3/7. Email:yaoalong@foxmail.com
 */
public class NetworkPoolGetReplication {
    @Test
    public void testSetFactorAndInitial() {
        NetworkPool networkPool = new NetworkPool();
        List<String> servers = new ArrayList<>();
        for (Integer i = 0; i < 3; i++) {
            servers.add("192.168.10.131:" + (2181 + i));
        }
        networkPool.setNumOfVirtualNode(3);
        networkPool.setAllServers(servers);

        for (Map.Entry<Long, String> entry : networkPool
                .getAllConsistentBuckets().entrySet()) {
            System.out.println(entry.getKey() + "::::" + entry.getValue());

        }
        for (String server : networkPool
                .getReplicationServer("192.168.10.131:2181")) {
            System.out.println("192.168.10.131:2181:" + server);
        }
        for (String server : networkPool
                .getReplicationServer("192.168.10.131:2182")) {
            System.out.println("192.168.10.131:2182:" + server);
        }
        for (String server : networkPool
                .getReplicationServer("192.168.10.131:2183")) {
            System.out.println("192.168.10.131:2183:" + server);
        }

    }
}
