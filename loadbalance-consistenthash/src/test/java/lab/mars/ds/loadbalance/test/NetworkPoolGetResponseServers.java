package lab.mars.ds.loadbalance.test;

import lab.mars.ds.loadbalance.impl.LoadBalanceConsistentHash;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Author:yaoalong. Date:2016/3/7. Email:yaoalong@foxmail.com
 */
public class NetworkPoolGetResponseServers {

    @Test
    public void testGetResponseServers() {
        LoadBalanceConsistentHash networkPool = new LoadBalanceConsistentHash();
        List<String> servers = new ArrayList<>();
        for (Integer i = 0; i < 3; i++) {
            servers.add("192.168.10.131:" + (2181 + i));
        }
        networkPool.setAllServers(servers);

        for (Map.Entry<Long, String> entry : networkPool
                .getAllConsistentBuckets().entrySet()) {
            System.out.println(entry.getKey() + "::::" + entry.getValue());

        }
        for (String server : networkPool
                .getReponseServers("192.168.10.131:2181")) {
            System.out.println("192.168.10.131:2181:" + server);
        }
        for (String server : networkPool
                .getReponseServers("192.168.10.131:2182")) {
            System.out.println("192.168.10.131:2182:" + server);
        }
        for (String server : networkPool
                .getReponseServers("192.168.10.131:2183")) {
            System.out.println("192.168.10.131:2183:" + server);
        }

    }
}
