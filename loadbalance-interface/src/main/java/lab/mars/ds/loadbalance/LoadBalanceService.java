package lab.mars.ds.loadbalance;

import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author:yaoalong. Date:2016/3/3. Email:yaoalong@foxmail.com
 */
public interface LoadBalanceService {

    void initialize();

    void setNumOfVirtualNode(Integer factor) throws LoadBalanceException;

    /**
     * 针对于key获取当前应该处理该key的server
     *
     * @param key
     * @return
     */
    String getServer(String key) throws LoadBalanceException;

    Integer getServerSize();

    /**
     * 获取某个server的处理范围
     *
     * @param server
     * @return
     */
    List<RangeDO> getRanges(String server);

    /**
     * 获取复制因子
     *
     * @return
     */
    Integer getReplication();

    /**
     * 获取对应的servers
     *
     * @param key
     * @return
     */
    List<String> getReplicationServers(String key);

    /**
     * 根据key的值获取第一个处理该server的hash值
     *
     * @param key
     * @return
     */
    Long getServerFirstPosition(String key) throws LoadBalanceException;

    /**
     * 获取key 的value得到对应的server
     *
     * @param value
     * @return
     */
    String getServer(Long value);

    /**
     * s 根据一个server获取它之前的所有server
     */
    List<String> getBeforeList(String server);

    /**
     * 获取server在环中的Hash位置
     *
     * @param server
     * @return
     */
    Long getFirstPosition(String server);

    /**
     * 获取特定server的下serverSize个server
     *
     * @param server
     * @param serverSize
     * @return
     */
    List<String> getNextServers(String server, Integer serverSize) throws LoadBalanceException;

    /**
     * 获取第n个server
     *
     * @param position
     * @return
     */
    String getServerByIndex(Long position);

    /**
     * 根据server以及factor获取负责的servers
     *
     * @param server
     * @return
     */
    List<String> getReplicationServer(String server);

    /**
     * 获取一个特定的server应该为哪些server提供复制功能
     *
     * @param server
     * @return
     */
    List<String> getReponseServers(String server);

    /**
     * 查看一个节点为另一个节点复制所提供的端口
     *
     * @param server
     * @param reponseServer
     * @return
     */
    long getServerResponseForAnthorSerer(String server, String reponseServer);

    /**
     * @param replicationFactor
     * @throws LoadBalanceException
     */

    void setReplicationFactor(Integer replicationFactor) throws LoadBalanceException;

    /**
     * 获取真正处理的节点
     */
    String getTrueServer(String key) throws LoadBalanceException;

    /**
     * 获取目前的存活列表
     *
     * @return
     */
    List<String> getServers();

    void setServers(List<String> servers) throws LoadBalanceException;

    TreeMap<Long, String> getConsistentBuckets();

    TreeMap<Long, String> getAllConsistentBuckets();

    ConcurrentHashMap<Long, String> getAllpositionToServer();

    List<String> getAllServers();


}
