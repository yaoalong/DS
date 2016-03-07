package lab.mars.ds.loadbalance;

import java.util.List;

/**
 * Author:yaoalong.
 * Date:2016/3/3.
 * Email:yaoalong@foxmail.com
 */
public interface NetworkInterface {


    void initialize();

    void setServers(List<String> servers);

    void setFactor(Integer factor);


    /**
     * 针对于key获取当前应该处理该key的server
     * @param key
     * @return
     */
    String getServer(String key);

    Integer getServerSize();

    /**
     * 获取某个server的处理范围
     * @param server
     * @return
     */
    List<RangeDO> getRanges(String server);

    /**
     * 获取复制因子
     * @return
     */
    Integer getReplication();

    /**
     * 获取对应的servers
     * @param key
     * @return
     */
    List<String> getReplicationServers(String key);

    /**
     * 根据key的值获取第一个处理该server的hash值
     * @param key
     * @return
     */
    Long getServerFirstPosition(String key);

    /**
     * 获取key 的value得到对应的server
     * @param value
     * @return
     */
    String getServer(Long value);
    /**s
     *根据一个server获取它之前的所有server
     */
    List<String> getBeforeList(String server );

    /**
     * 获取server在环中的Hash位置
     * @param server
     * @return
     */
    Long getFirstPosition(String server);

    /**
     * 获取特定serverSize的下serverSize个server
     * @param server
     * @param serverSize
     * @return
     */
    List<String> getNextServers(String server,Integer serverSize);

    /**
     * 获取第n个server
     * @param position
     * @return
     */
    String getServerByIndex(Long position);

    /**
     * 根据server以及factor获取负责的servers
     * @param server
     * @param factor
     * @return
     */
    List<String> getReplicationServer(String server,Integer factor);

    /**
     * 获取一个特定的server应该为哪些server提供复制功能
     * @param server
     * @return
     */
    List<String> getReponseServers(String server);

    /**
     * 查看一个节点为另一个节点复制所提供的端口
     * @param server
     * @param reponseServer
     * @return
     */
    long getServerResponseForAnthorSerer(String server,String reponseServer);


}
