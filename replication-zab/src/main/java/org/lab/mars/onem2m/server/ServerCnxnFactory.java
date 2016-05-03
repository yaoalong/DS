/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lab.mars.onem2m.server;

import lab.mars.ds.loadbalance.impl.NetworkPool;
import org.lab.mars.onem2m.server.quorum.M2mQuorumPeerMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ServerCnxnFactory {

    public static final String ZOOKEEPER_SERVER_CNXN_FACTORY = "zookeeper.serverCnxnFactory";
    /**
     * The buffer will cause the connection to be close when we do a send.
     */
    protected final HashSet<ServerCnxn> cnxns = new HashSet<ServerCnxn>();

    /**
     * 多个ZooKeeper 不同的ip对应的是不同的ZooKeeperServer
     */
    protected ConcurrentHashMap<String, ZooKeeperServer> zkServers = new ConcurrentHashMap<String, ZooKeeperServer>();
    protected M2mQuorumPeerMain m2mQuorumPeerMain;

    protected  Long packetCount=0L;
    Logger LOG = LoggerFactory.getLogger(ServerCnxnFactory.class);

    public ServerCnxnFactory(M2mQuorumPeerMain m2mQuorumPeerMain) {
        this.m2mQuorumPeerMain = m2mQuorumPeerMain;
    }

    static public ServerCnxnFactory createFactory() throws IOException {
        String serverCnxnFactoryName = System
                .getProperty(ZOOKEEPER_SERVER_CNXN_FACTORY);
        try {
            return (ServerCnxnFactory) Class.forName(serverCnxnFactoryName)
                    .newInstance();
        } catch (Exception e) {
            IOException ioe = new IOException("Couldn't instantiate "
                    + serverCnxnFactoryName);
            ioe.initCause(e);
            throw ioe;
        }
    }

    static public ServerCnxnFactory createFactory(int clientPort,
                                                  int maxClientCnxns) throws IOException {
        return createFactory(new InetSocketAddress(clientPort), maxClientCnxns);
    }

    static public ServerCnxnFactory createFactory(InetSocketAddress addr,
                                                  int maxClientCnxns) throws IOException {
        ServerCnxnFactory factory = createFactory();
        factory.configure(addr, maxClientCnxns);
        return factory;
    }

    public abstract int getLocalPort();

    public abstract Iterable<ServerCnxn> getConnections();

    public int getNumAliveConnections() {
        synchronized (cnxns) {
            return cnxns.size();
        }
    }

    public abstract void closeSession(long sessionId);

    public abstract void configure(InetSocketAddress addr, int maxClientCnxns)
            throws IOException;

    /**
     * Maximum number of connections allowed from particular host (ip)
     */
    public abstract int getMaxClientCnxnsPerHost();

    /**
     * Maximum number of connections allowed from particular host (ip)
     */
    public abstract void setMaxClientCnxnsPerHost(int max);

    public abstract void startup(ZooKeeperServer zkServer) throws IOException,
            InterruptedException;

    public abstract void join() throws InterruptedException;

    public abstract void shutdown();

    public abstract void start();

    public abstract String getMyIp();

    /**
     * 每个特定的QuorumPeer都会添加自己的ZooKeeperServer
     *
     * @param ip
     * @param zooKeeperServer
     */
    final public void addZooKeeperServer(String ip,
                                         ZooKeeperServer zooKeeperServer) {
        this.zkServers.put(ip, zooKeeperServer);
        zooKeeperServer.setServerCnxnFactory(this);
        if (ip.equals(getMyIp())) {
            m2mQuorumPeerMain.isStarted = true;
        }
    }

    /**
     * 删除特定Ip的ZooKeeper
     *
     * @param ip
     */
    final public void removeZookeeper(String ip) {
//        if (ip.equals(getMyIp())) {
//            m2mQuorumPeerMain.isStarted = false;
//        }
        this.zkServers.remove(ip);
    }

    public abstract void closeAll();

    public abstract InetSocketAddress getLocalAddress();

    public abstract NetworkPool getNetworkPool();

    public abstract Integer getReplicationFactor();

    public ConcurrentHashMap<String, ZooKeeperServer> getZkServers() {
        return zkServers;
    }

    public void addPacketCount(){
        synchronized (packetCount){
            packetCount++;
        }
    }
    public Long getPacketCount(){
        synchronized (packetCount){
            return packetCount;
        }
    }

}
