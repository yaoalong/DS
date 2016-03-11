package lab.mars.ds.loadbalance.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import lab.mars.ds.loadbalance.NetworkInterface;
import lab.mars.ds.loadbalance.RangeDO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Author:yaoalong. Date:2016/3/3. Email:yaoalong@foxmail.com
 */
public class NetworkPool implements NetworkInterface {

    private static Logger LOG = LoggerFactory.getLogger(NetworkPool.class);
    private static ThreadLocal<MessageDigest> MD5 = new ThreadLocal<MessageDigest>() {
        @Override
        protected final MessageDigest initialValue() {
            try {
                return MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                if (LOG.isErrorEnabled())
                    LOG.error("++++ no md5 algorithm found");
                throw new IllegalStateException("++++ no md5 algorythm found");
            }
        }
    };
    private final ConcurrentHashMap<String, Long> serverFirstToHash = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Long> serverFirstToPosition = new ConcurrentHashMap<>();
    private TreeMap<Long, String> consistentBuckets;
    private TreeMap<Long, String> allConsistentBuckets;
    private int numOfVirtualNode = 1;
    private volatile boolean initialized = false;
    private List<String> servers;
    private ConcurrentHashMap<Long, String> allpositionToServer = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Long> allserverToPosition = new ConcurrentHashMap<>();
    // 一个服务器负责提供数据复制服务器的列表
    private ConcurrentHashMap<String, List<String>> serverResponseServers = new ConcurrentHashMap<>();

    private Integer replicationFactor = 2;

    /**
     * 计算一个key的hash值
     *
     * @param key
     * @return
     */
    public static long md5HashingAlg(String key) {
        MessageDigest md5 = MD5.get();
        md5.reset();
        md5.update(key.getBytes());
        byte[] bKey = md5.digest();
        long res = ((long) (bKey[3] & 0xFF) << 24)
                | ((long) (bKey[2] & 0xFF) << 16)
                | ((long) (bKey[1] & 0xFF) << 8) | (long) (bKey[0] & 0xFF);
        return res;
    }

    /*
     * 初始化
     */
    public synchronized void initialize() {
        try {

            // if servers is not set, or it empty, then
            // throw a runtime exception
            if (servers == null || servers.size() <= 0) {
                if (LOG.isErrorEnabled())
                    LOG.error("++++ trying to initialize with no servers");
                throw new IllegalStateException(
                        "++++ trying to initialize with no servers");
            }

            // only create up to maxCreate connections at once

            // initalize our internal hashing structures
            populateConsistentBuckets();
        } catch (Exception ex) {
            LOG.error("error occur:{}", ex.getMessage());
        }
    }

    public void populateConsistentBuckets() {
        TreeMap<Long, String> newConsistentBuckets = new TreeMap<Long, String>();
        MessageDigest md5 = MD5.get();

        for (int i = 0; i < servers.size(); i++) {
            for (long j = 0; j < numOfVirtualNode; j++) {
                byte[] d = md5.digest((servers.get(i) + "-" + j).getBytes());
                for (int h = 0; h < 1; h++) {
                    Long k = ((long) (d[3 + h * 4] & 0xFF) << 24)
                            | ((long) (d[2 + h * 4] & 0xFF) << 16)
                            | ((long) (d[1 + h * 4] & 0xFF) << 8)
                            | ((long) (d[0 + h * 4] & 0xFF));

                    newConsistentBuckets.put(k, servers.get(i));
                }
            }
        }
        this.consistentBuckets = newConsistentBuckets;
        initialized = true;
    }

    @Override
    public synchronized void setServers(List<String> servers) {
        this.servers = servers;
    }

    public synchronized void setAllServers(List<String> allServers) {
        TreeMap<Long, String> newConsistentBuckets = new TreeMap<Long, String>();
        MessageDigest md5 = MD5.get();

        for (int i = 0; i < allServers.size(); i++) {
            for (long j = 0; j < numOfVirtualNode; j++) {
                byte[] d = md5.digest((allServers.get(i) + "-" + j).getBytes());
                for (int h = 0; h < 1; h++) {
                    Long k = ((long) (d[3 + h * 4] & 0xFF) << 24)
                            | ((long) (d[2 + h * 4] & 0xFF) << 16)
                            | ((long) (d[1 + h * 4] & 0xFF) << 8)
                            | ((long) (d[0 + h * 4] & 0xFF));

                    newConsistentBuckets.put(k, allServers.get(i));
                }
            }
        }
        long position = 0;
        for (Map.Entry<Long, String> map : newConsistentBuckets.entrySet()) {

            if (!serverFirstToHash.contains(map.getValue())) {
                serverFirstToHash.put(map.getValue(), map.getKey());
            }
            allserverToPosition.put(map.getValue(), position);
            allpositionToServer.put(position, map.getValue());
            if (!serverFirstToPosition.contains(map.getValue())) {
                serverFirstToPosition.put(map.getValue(), position);
            }
            position++;
        }
        this.allConsistentBuckets = newConsistentBuckets;
        for (Map.Entry<String, Long> map : serverFirstToHash.entrySet()) {
            List<String> servers = getResponseServers(map.getKey());
            serverResponseServers.put(map.getKey(), servers);

        }

    }

    @Override
    public void setNumOfVirtualNode(Integer numOfVirtualNode) {
        this.numOfVirtualNode = numOfVirtualNode;
    }

    @Override
    public synchronized String getServer(String key) {
        while (initialized == false) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return consistentBuckets.get(getBucket(key));
    }

    @Override
    public String getTrueServer(String key) {
        return allConsistentBuckets.get(getAllBucket(key));

    }

    @Override
    public Integer getServerSize() {
        return consistentBuckets.size();
    }

    @Override
    public List<RangeDO> getRanges(String server) {
        List<RangeDO> result = new ArrayList<>();
        if (allConsistentBuckets.isEmpty()) {
            return result;
        }
        long pre = 0;
        for (Map.Entry<Long, String> entry : allConsistentBuckets.entrySet()) {

            if (server.equals(entry.getValue())) {
                RangeDO rangeDO = new RangeDO();
                rangeDO.setStart(pre);
                rangeDO.setEnd(entry.getKey());
                result.add(rangeDO);
            }

            pre = entry.getKey();
        }
        if (server.equals(allConsistentBuckets.firstEntry().getValue())) {
            Map.Entry<Long, String> end = allConsistentBuckets.lastEntry();
            RangeDO rangeDO = new RangeDO();
            rangeDO.setStart(end.getKey());
            rangeDO.setEnd(Long.MAX_VALUE);
            result.add(rangeDO);
        }
        return result;
    }

    @Override
    public Integer getReplication() {
        return numOfVirtualNode;
    }

    @Override
    public List<String> getReplicationServers(String server) {

        long firstLong = serverFirstToHash.get(server);
        List<String> result = new ArrayList<>();
        while (result.size() < replicationFactor - 1) {
            long temp = findAllPointFor(firstLong + 1);
            String positionServer = consistentBuckets.get(temp);
            if (!result.contains(positionServer)
                    && !server.equals(positionServer)) {
                result.add(positionServer);
            }
        }
        return result;
    }

    /**
     * @param key
     * @return
     */
    @Override
    public Long getServerFirstPosition(String key) {
        return getAllBucket(key);
    }

    @Override
    public synchronized String getServer(Long value) {
        return consistentBuckets.get(findPointFor(value));
    }

    @Override
    public List<String> getBeforeList(String server) {

        Long position = allserverToPosition.get(server);
        List<String> result = new ArrayList<>();
        for (long i = 0; i < position; i++) {
            result.add(allpositionToServer.get(position + i));
        }
        return result;
    }

    @Override
    public Long getFirstPosition(String server) {
        return serverFirstToHash.get(server);
    }

    @Override
    public List<String> getNextServers(String server, Integer serverSize) {
        Long position = allserverToPosition.get(server);
        List<String> result = new ArrayList<>();
        for (int i = 0; i < serverSize; i++) {
            result.add(allpositionToServer.get(position + i));
        }
        return result;
    }

    @Override
    public String getServerByIndex(Long position) {
        return allpositionToServer.get(position);
    }

    @Override
    public List<String> getReplicationServer(String server) {
        long firstLong = serverFirstToHash.get(server);
        List<String> result = new ArrayList<>();
        result.add(server);
        while (result.size() < replicationFactor) {
            long temp = findAllPointFor(firstLong + 1);
            String positionServer = allConsistentBuckets.get(temp);
            if (!result.contains(positionServer)
                    && !server.equals(positionServer)) {
                result.add(positionServer);
            }
        }
        return result;
    }

    @Override
    public List<String> getReponseServers(String server) {
        return serverResponseServers.get(server);
    }

    /**
     * 判断一个服务器和另一个服务器的距离
     */
    @Override
    public long getServerResponseForAnthorSerer(String server,
            String reponseServer) {
        Long position = serverFirstToPosition.get(server);
        long responsePosition = serverFirstToPosition.get(reponseServer);
        return responsePosition - position;
    }

    private final long getBucket(String key) {
        long hc = md5HashingAlg(key);
        long result = findPointFor(hc);
        return result;
    }

    private final Long findPointFor(Long hv) {
        synchronized (this.consistentBuckets) {
            SortedMap<Long, String> tmap = this.consistentBuckets.tailMap(hv);

            return (tmap.isEmpty()) ? this.consistentBuckets.firstKey() : tmap
                    .firstKey();
        }

    }

    /**
     * 获取一个server应该对外提供的server
     * 
     * @param server
     * @return
     */
    private List<String> getResponseServers(String server) {
        List<String> responseServers = new ArrayList<String>();
        for (Entry<String, Long> index : serverFirstToHash.entrySet()) {
            List<String> resultList = getReplicationServer(index.getKey());
            if (resultList.contains(server)) {
                responseServers.add(index.getKey());
            }
        }

        return responseServers;
    }

    private final long getAllBucket(String key) {
        long hc = md5HashingAlg(key);
        long result = findAllPointFor(hc);
        return result;
    }

    private final Long findAllPointFor(Long hv) {
        synchronized (this.allConsistentBuckets) {
            SortedMap<Long, String> tmap = this.allConsistentBuckets
                    .tailMap(hv);

            return (tmap.isEmpty()) ? this.allConsistentBuckets.firstKey()
                    : tmap.firstKey();
        }
    }

    @Override
    public void setReplicationFactor(Integer replicationFactor) {
        this.replicationFactor = replicationFactor;

    }

    @Override
    public List<String> getServers() {
        return servers;
    }

    public TreeMap<Long, String> getConsistentBuckets() {
        return consistentBuckets;
    }

    public void setConsistentBuckets(TreeMap<Long, String> consistentBuckets) {
        this.consistentBuckets = consistentBuckets;
    }

    public TreeMap<Long, String> getAllConsistentBuckets() {
        return allConsistentBuckets;
    }

    public void setAllConsistentBuckets(
            TreeMap<Long, String> allConsistentBuckets) {
        this.allConsistentBuckets = allConsistentBuckets;
    }

}
