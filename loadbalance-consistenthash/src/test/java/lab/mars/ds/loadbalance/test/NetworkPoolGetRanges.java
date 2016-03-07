package lab.mars.ds.loadbalance.test;

import lab.mars.ds.loadbalance.RangeDO;
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
public class NetworkPoolGetRanges {
    @Test
    public void testGetRanges(){
        NetworkPool networkPool = new NetworkPool();
        List<String> servers = new ArrayList<>();
        for (Integer i = 0; i < 3; i++) {
            servers.add("192.168.10.131:" + (2181 + i));
        }
        networkPool.setFactor(3);
        networkPool.setAllServers(servers);

        for (Map.Entry<Long, String> entry : networkPool.allConsistentBuckets.entrySet()) {
            System.out.println(entry.getKey() + "::::" + entry.getValue());

        }
       for(RangeDO rangeDO:networkPool.getRanges("192.168.10.131:2181")){
            System.out.println("192.168.10.131:2181 handle :"+rangeDO.getStart()+" end:"+rangeDO.getEnd());
        }
        for(RangeDO rangeDO:networkPool.getRanges("192.168.10.131:2182")){
            System.out.println("192.168.10.131:2182 handle :"+rangeDO.getStart()+" end:"+rangeDO.getEnd());
        }
        for(RangeDO rangeDO:networkPool.getRanges("192.168.10.131:2183")){
            System.out.println("192.168.10.131:2183 handle :"+rangeDO.getStart()+" end:"+rangeDO.getEnd());
        }
    }
}
