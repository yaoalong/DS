package lab.mars.ds.loadbalance.test;

import lab.mars.ds.loadbalance.LoadBalanceException;
import lab.mars.ds.loadbalance.impl.LoadBalanceConsistentHash;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Author:yaoalong.
 * Date:2016/3/17.
 * Email:yaoalong@foxmail.com
 */
public class NetworkPoolGetNextServers {
    public static final LoadBalanceConsistentHash network = new LoadBalanceConsistentHash();

    static {
        List<String> servers = new ArrayList<String>();

        for (int i = 0; i < 10; i++) {
            servers.add("192.168.10.13" + i);
        }
        network.setAllServers(servers);
    }

    public static void printALine() {
        System.out.println("***************");
    }

    @Test
    public void testNextServers() throws LoadBalanceException {
        network.getAllConsistentBuckets().forEach((key, value) -> {
            System.out.println("key:  " + key + "  value " + value);
        });
        List<String> servers = network.getNextServers("192.168.10.135", 10);
        printALine();
        servers.forEach(t -> {
            System.out.println("server:" + t);
        });
        List<String> beforeServers = network.getBeforeList("192.168.10.131");
        printALine();
        beforeServers.forEach(t -> {
            System.out.println("before server:" + t);
        });

    }
}
