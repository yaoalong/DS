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

import lab.mars.ds.loadbalance.LoadBalanceException;
import lab.mars.ds.loadbalance.LoadBalanceException.Code;
import lab.mars.ds.loadbalance.LoadBalanceException.ServerSizeTooBig;
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

    private List<String> allServers;

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

        this.consistentBuckets = getConsistentBuckets(servers);
        initialized = true;
    }

    public TreeMap<Long, String> getConsistentBuckets(List<String> servers) {
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
        return newConsistentBuckets;
    }

    public synchronized void setAllServers(List<String> allServers) {
        this.allServers = allServers;
        TreeMap<Long, String> newConsistentBuckets = getConsistentBuckets(allServers);
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
    public void setNumOfVirtualNode(Integer numOfVirtualNode)
            throws LoadBalanceException {
        if (numOfVirtualNode == null || numOfVirtualNode < 1) {
            throw new LoadBalanceException(Code.NUM_OF_VIRTURAL_NODE_IS_RROR,
                    "num of virtual node is error");
        }
        this.numOfVirtualNode = numOfVirtualNode;
    }

    @Override
    public synchronized String getServer(String key)
            throws LoadBalanceException {
        if (key == null || key.isEmpty()) {
            throw new LoadBalanceException(Code.KEY_PARAM_NULL, "key is null");
        }
        if (consistentBuckets == null || consistentBuckets.isEmpty()) {
            throw new LoadBalanceException(Code.SERGERS_IS_NOT_INIT,
                    "servers is not init");
        }
        return consistentBuckets.get(getBucket(key));
    }

    @Override
    public String getTrueServer(String key) throws LoadBalanceException {

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

                RangeDO rangeDO = new RangeDO(pre, entry.getKey());
                result.add(rangeDO);
            }

            pre = entry.getKey();
        }
        if (server.equals(allConsistentBuckets.firstEntry().getValue())) {
            Map.Entry<Long, String> end = allConsistentBuckets.lastEntry();
            RangeDO rangeDO = new RangeDO(end.getKey(), Long.MAX_VALUE);
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
        serverFirstToHash.forEach((s, t) -> {
            System.out.println("S:" + s);
            System.out.println("t:" + t);
        });
        long firstLong = serverFirstToHash.get(server);

        System.out.println("repliccationFactor:" + replicationFactor);
        List<String> result = new ArrayList<>();

        while (result.size() < replicationFactor - 1) {
            long temp = findAllPointFor(firstLong + 1);
            String positionServer = consistentBuckets.get(temp);
            if (!result.contains(positionServer)
                    && !server.equals(positionServer)) {
                result.add(positionServer);
            }
        }
        result.add(server);
        return result;
    }

    /**
     * @param key
     * @return
     */
    @Override
    public Long getServerFirstPosition(String key) throws LoadBalanceException {
        if (key == null || key.isEmpty()) {
            throw new LoadBalanceException(Code.KEY_PARAM_NULL,
                    "key can't is null");
        }
        return getAllBucket(key);
    }

    @Override
    public synchronized String getServer(Long value) {
        return consistentBuckets.get(findPointFor(value));
    }

    // TODO 加上错误处理
    @Override
    public List<String> getBeforeList(String server) {
        System.out.println("get server:" + server);
        Long position = allserverToPosition.get(server);
        List<String> result = new ArrayList<>();
        for (long i = 0; i < position; i++) {
            result.add(allpositionToServer.get(i));
        }
        return result;
    }

    @Override
    public Long getFirstPosition(String server) {
        return serverFirstToHash.get(server);
    }

    @Override
    public List<String> getNextServers(String server, Integer serverSize)
            throws LoadBalanceException {
        if (server == null || server.isEmpty()) {
            throw new LoadBalanceException(Code.SERVER_IS_NULL,
                    "server is null");
        }
        if (serverSize == null || serverSize <= 0) {
            throw new LoadBalanceException(Code.SERVERSIZE_IS_ERROR,
                    "server size is error");
        }
        if (serverSize > allpositionToServer.size() - 1) {
            throw new ServerSizeTooBig();
        }
        Long position = allserverToPosition.get(server);
        List<String> result = new ArrayList<>();
        for (int i = 1; i <= serverSize; i++) {
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

    private final long getAllBucket(String key) throws LoadBalanceException {
        if (key == null || key.isEmpty()) {
            throw new LoadBalanceException(Code.KEY_PARAM_NULL,
                    "key can't is null");
        }
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
    public void setReplicationFactor(Integer replicationFactor)
            throws LoadBalanceException {
        if (replicationFactor == null || replicationFactor < 2) {
            throw new LoadBalanceException(Code.REPLICATION_FACTOR_PARAM_ERROR,
                    "复制因子不能小于2");
        }
        this.replicationFactor = replicationFactor;
    }

    @Override
    public List<String> getServers() {
        return servers;
    }

    @Override
    public synchronized void setServers(List<String> servers)
            throws LoadBalanceException {
        if (servers == null || servers.isEmpty()) {
            throw new LoadBalanceException(Code.SERVER_IS_NULL,
                    "cant set null servers");
        }
        servers.forEach(t -> System.out.println("server:" + t));
        this.servers = servers;
        initialize();
    }

    @Override
    public TreeMap<Long, String> getConsistentBuckets() {
        return consistentBuckets;
    }

    @Override
    public TreeMap<Long, String> getAllConsistentBuckets() {
        return allConsistentBuckets;
    }

    @Override
    public ConcurrentHashMap<Long, String> getAllpositionToServer() {
        return allpositionToServer;
    }

    @Override
    public List<String> getAllServers() {
        return allServers;
    }

}
