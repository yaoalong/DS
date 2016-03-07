package lab.mars.ds.loadbalance.test;

import lab.mars.ds.loadbalance.impl.NetworkPool;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Author:yaoalong.
 * Date:2016/3/7.
 * Email:yaoalong@foxmail.com
 */
public class NetworkPoolGetServer {

    @Test
    public void testGetServerWithoutFactor() {

        NetworkPool networkPool = new NetworkPool();
        List<String> servers = new ArrayList<String>();
        for (Integer i = 0; i < 3; i++) {
            servers.add("192.168.10.131:" + (2181 + i));
        }
        networkPool.setServers(servers);
        networkPool.initialize();

        for (Map.Entry<Long, String> entry : networkPool.consistentBuckets.entrySet()) {
            System.out.println(entry.getKey() + "::::" + entry.getValue());

        }
        System.out.println("key hash:" + networkPool.md5HashingAlg("yaoalong"));

        System.out.println("get Server for key 'yaoalong':" + networkPool.getServer("yaoalong"));
    }
    @Test
    public void testGetServerWithFactor() {

        NetworkPool networkPool = new NetworkPool();
        List<String> servers = new ArrayList<>();
        for (Integer i = 0; i < 3; i++) {
            servers.add("192.168.10.131:" + (2181 + i));
        }
        networkPool.setFactor(3);
        networkPool.setServers(servers);
        networkPool.initialize();

        for (Map.Entry<Long, String> entry : networkPool.consistentBuckets.entrySet()) {
            System.out.println(entry.getKey() + "::::" + entry.getValue());

        }
        System.out.println("key hash:" + networkPool.md5HashingAlg("2522"));


        System.out.println("get Server for key 'yaoalong':" + networkPool.getServer("2522"));
    }
}
