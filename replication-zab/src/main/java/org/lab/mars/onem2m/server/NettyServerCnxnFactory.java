/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lab.mars.onem2m.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;

import lab.mars.ds.loadbalance.impl.NetworkPool;
import lab.mars.ds.network.TcpServer;

import lab.mars.ds.util.Statistics;
import org.lab.mars.onem2m.server.quorum.M2mHandler;
import org.lab.mars.onem2m.server.quorum.M2mQuorumPeerMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyServerCnxnFactory extends ServerCnxnFactory {
    private static final Logger LOG = LoggerFactory
            .getLogger(NettyServerCnxnFactory.class);
    private Integer clientPort;
    private TcpServer tcpServer;
    /*
     * 获取本机的ip地址
     */
    private String myIp;
    private NetworkPool networkPool;
    private Integer replicationFactor;
    private String myWebIp;

    private HashMap<Long,String> webServers;
     public NettyServerCnxnFactory(M2mQuorumPeerMain m2mQuorumPeerMain) {
        super(m2mQuorumPeerMain);
    }

    @Override
    public int getLocalPort() {
        return clientPort;
    }

    @Override
    public Iterable<ServerCnxn> getConnections() {
        return null;
    }

    @Override
    public void closeSession(long sessionId) {

    }

    public void configure(Integer clientPort, int maxClientCnxns,
            M2mHandler m2mHandler, Integer numberOfConnections)
            throws IOException {
        this.clientPort = clientPort;
        tcpServer = new TcpServer(this, m2mHandler, numberOfConnections);

    }

    @Override
    public int getMaxClientCnxnsPerHost() {
        return 0;
    }

    @Override
    public void setMaxClientCnxnsPerHost(int max) {

    }

    public void startup() throws IOException, InterruptedException {
        start();
    }

    @Override
    public void join() throws InterruptedException {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void start() {

        LOG.info("binding to port: " + clientPort);
        try {
            tcpServer.bind(myIp, clientPort);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void closeAll() {
        tcpServer.close();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return null;
    }

    @Override
    public void configure(InetSocketAddress addr, int maxClientCnxns)
            throws IOException {

    }

    @Override
    public void startup(ZooKeeperServer zkServer) throws IOException,
            InterruptedException {

    }

    public String getMyIp() {
        return myIp + ":" + getLocalPort();
    }

    public void setMyIp(String myIp) {
        this.myIp = myIp;
    }

    public NetworkPool getNetworkPool() {
        return networkPool;
    }

    public void setNetworkPool(NetworkPool networkPool) {
        this.networkPool = networkPool;
    }

    public String getMyWebIp() {
        return myWebIp;
    }

    public void setMyWebIp(String myWebIp) {
        this.myWebIp = myWebIp;
    }

    @Override
    public Integer getReplicationFactor() {
        return replicationFactor;
    }

    public void setReplicationFactory(Integer replicationFactor) {
        this.replicationFactor = replicationFactor;

    }

    public HashMap<Long, String> getWebServer() {
        return webServers;
    }

    public void setWebServer(HashMap<Long, String> webServer) {
        this.webServers = webServer;
    }
}