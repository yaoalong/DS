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

package org.lab.mars.onem2m.server.quorum;

import lab.mars.ds.loadbalance.LoadBalanceException;
import lab.mars.ds.loadbalance.impl.LoadBalanceConsistentHash;
import lab.mars.ds.persistence.DSDatabaseImpl;
import lab.mars.ds.persistence.DSDatabaseInterface;
import org.lab.mars.onem2m.server.ZooKeeperServer;
import org.lab.mars.onem2m.server.quorum.M2mQuorumPeer.QuorumServer;
import org.lab.mars.onem2m.server.quorum.flexible.M2mQuorumMaj;
import org.lab.mars.onem2m.server.quorum.flexible.M2mQuorumVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.Map.Entry;

public class QuorumPeerConfig {
    private static final Logger LOG = LoggerFactory
            .getLogger(QuorumPeerConfig.class);
    public final HashMap<Long, QuorumServer> servers = new HashMap<Long, QuorumServer>(); // sid、服务器选举的配置信息
    /**
     * 一个服务器对应的sid
     */

    public final Map<String, Long> allServerToSids = new HashMap<String, Long>();
    /**
     * 将所有机器拆分成不同的zab集群
     */
    protected final List<HashMap<Long, QuorumServer>> quorumServersList = new ArrayList<>();
    public InetSocketAddress clientPortAddress;
    public String dataDir;
    public String dataLogDir;
    public int tickTime = ZooKeeperServer.DEFAULT_TICK_TIME;
    public int maxClientCnxns = 60;
    /**
     * defaults to -1 if not set explicitly
     */
    public int maxSessionTimeout = -1;
    public int initLimit;
    public int syncLimit;
    public int electionAlg = 3;
    public int electionPort = 2182;
    public long serverId;
    public String myIp;
    public M2mQuorumVerifier quorumVerifier;
    public int snapRetainCount = 3;
    public boolean syncEnabled = true;
    public DSDatabaseInterface m2mDataBase;
    public Boolean cleaned = false;
    public String node = "127.0.0.1";
    public String keySpace = "tests";
    public String table = "student";
    public String zooKeeperServer;
    public Integer replication_factor;
    /**
     * 用这个来判断自己在环中的位置
     */
    public LoadBalanceConsistentHash networkPool;
    public List<M2mAddressToId> addressToSid = new ArrayList<>();
    public List<String> allServerStrings = new ArrayList<String>();
    public Integer numOfVirtualNode;
    public Integer numberOfConnections;
    public Integer zabClientPort;
    public String zooKeeperServerString;
    public HashMap<Long, Integer> sidAndWebPort = new HashMap<Long, Integer>();
    /**
     * defaults to -1 if not set explicitly
     */
    protected int minSessionTimeout = -1;
    protected  List<String> webServers=new ArrayList<>();
    protected HashMap<Long, String> webIdToServers = new HashMap<>();
    M2mQuorumServer m2mQuorumServers = new M2mQuorumServer();
    /**
     * 不同机器实例对应的客户端端口号
     */
    private HashMap<Long, Integer> sidToClientPort = new HashMap<>();
    private Integer webPort;

    /**
     * Parse a ZooKeeper configuration file
     *
     * @param path the patch of the configuration file
     * @throws ConfigException      error processing configuration
     * @throws LoadBalanceException
     */
    public void parse(String path) throws ConfigException, LoadBalanceException {
        File configFile = new File(path);

        LOG.info("Reading configuration from: " + configFile);

        try {
            if (!configFile.exists()) {
                throw new IllegalArgumentException(configFile.toString()
                        + " file is missing");
            }

            Properties cfg = new Properties();
            FileInputStream in = new FileInputStream(configFile);
            try {
                cfg.load(in);
            } finally {
                in.close();
            }

            parseProperties(cfg);
        } catch (IOException e) {
            throw new ConfigException("Error processing " + path, e);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            throw new ConfigException("Error processing " + path, e);
        }
    }

    /**
     * Parse config from a Properties.
     *
     * @param zkProp Properties to parse from.
     * @throws IOException
     * @throws ConfigException
     * @throws LoadBalanceException
     */
    public void parseProperties(Properties zkProp) throws IOException,
            ConfigException, LoadBalanceException {
        String clientPortAddress = null;
        for (Entry<Object, Object> entry : zkProp.entrySet()) {
            String key = entry.getKey().toString().trim();
            String value = entry.getValue().toString().trim();
            if (key.equals("dataDir")) {
                dataDir = value;
            } else if (key.equals("dataLogDir")) {
                dataLogDir = value;
            } else if (key.equals("clientPortAddress")) {
                clientPortAddress = value.trim();
            } else if (key.equals("tickTime")) {
                tickTime = Integer.parseInt(value);
            } else if (key.equals("initLimit")) {
                initLimit = Integer.parseInt(value);
            } else if (key.equals("syncLimit")) {
                syncLimit = Integer.parseInt(value);
            } else if (key.equals("syncEnabled")) {
                syncEnabled = Boolean.parseBoolean(value);
            } else if (key.equals("autopurge.snapRetainCount")) {
                snapRetainCount = Integer.parseInt(value);
            } else if (key.startsWith("zabServer.")) {
                int dot = key.indexOf('.');
                long sid = Long.parseLong(key.substring(dot + 1));
                String parts[] = value.split(":");
                if ((parts.length != 2) && (parts.length != 3)
                        && (parts.length != 4)) {
                    LOG.error(value
                            + " does not have the form host:port or host:port:port "
                            + " or host:port:port:type");
                }
                InetSocketAddress addr = new InetSocketAddress(parts[0],
                        Integer.parseInt(parts[1]));
                addressToSid.add(new M2mAddressToId(sid, parts[0]));
                if (parts.length == 2) {
                    servers.put(Long.valueOf(sid), new QuorumServer(sid, addr));
                } else if (parts.length == 3) {
                    InetSocketAddress electionAddr = new InetSocketAddress(
                            parts[0], Integer.parseInt(parts[2]));// 用来选举的信息
                    servers.put(Long.valueOf(sid), new QuorumServer(sid, addr,
                            electionAddr));
                }
            } else if (key.equals("cassandra.node")) {
                node = value;
            } else if (key.equals("cassandra.keyspace")) {
                keySpace = value;
            } else if (key.equals("cassandra.table")) {
                table = value;
            } else if (key.equals("cassandra.cleaned")) {
                cleaned = Boolean.valueOf(value);
            } else if (key.equals("zooKeeper.server")) {
                zooKeeperServer = value;
            } else if (key.equals("replication.factor")) {
                replication_factor = Integer.valueOf(value);

            } else if (key.equals("numOfVirtualNode")) {
                numOfVirtualNode = Integer.valueOf(value);
            } else if (key.startsWith("zabPort")) {
                int dot = key.indexOf('.');
                long sid = Long.parseLong(key.substring(dot + 1));
                sidToClientPort.put(sid, Integer.valueOf(value));
            } else if (key.equals("zabClientPort")) {
                zabClientPort = Integer.valueOf(value);
            } else if (key.equals("numberOfConnections")) {
                numberOfConnections = Integer.valueOf(value);
            } else if (key.equals("zooKeeperServer")) {
                System.out.println("ZooKeeperServer:" + value);
                zooKeeperServerString = value;
            } else if (key.startsWith("webPort")) {
                int dot = key.indexOf('.');
                long sid = Long.parseLong(key.substring(dot + 1));
                webPort = Integer.valueOf(value);
                sidAndWebPort.put(sid, webPort);
            } else if (key.startsWith("webServer")) {
                int dot = key.indexOf('.');
                long sid = Long.parseLong(key.substring(dot + 1));
                webIdToServers.put(sid, value);
                webServers.add(value);
            } else {
                System.setProperty("zookeeper." + key, value);
            }
        }

        if (dataDir == null) {
            throw new IllegalArgumentException("dataDir is not set");
        }
        if (dataLogDir == null) {
            dataLogDir = dataDir;
        } else {
            if (!new File(dataLogDir).isDirectory()) {
                throw new IllegalArgumentException("dataLogDir " + dataLogDir
                        + " is missing.");
            }
        }
        if (zabClientPort == 0) {
            throw new IllegalArgumentException("clientPort is not set");
        }
        if (clientPortAddress != null) {
            this.clientPortAddress = new InetSocketAddress(
                    InetAddress.getByName(clientPortAddress), zabClientPort);
        } else {
            this.clientPortAddress = new InetSocketAddress(zabClientPort);
        }

        if (tickTime == 0) {
            throw new IllegalArgumentException("tickTime is not set");
        }
        if (minSessionTimeout > maxSessionTimeout) {
            throw new IllegalArgumentException(
                    "minSessionTimeout must not be larger than maxSessionTimeout");
        }
        if (servers.size() == 0) {
            return;
        } else if (servers.size() == 1) {

            // HBase currently adds a single server line to the config, for
            // b/w compatibility reasons we need to keep this here.
            LOG.error("Invalid configuration, only one server specified (ignoring)");
            servers.clear();
        } else if (servers.size() > 1) {
            if (servers.size() == 2) {
                LOG.warn("No server failure will be tolerated. "
                        + "You need at least 3 servers.");
            } else if (servers.size() % 2 == 0) {
                LOG.warn("Non-optimial configuration, consider an odd number of servers.");
            }
            if (initLimit == 0) {
                throw new IllegalArgumentException("initLimit is not set");
            }
            if (syncLimit == 0) {
                throw new IllegalArgumentException("syncLimit is not set");
            }
            /*
             * If using FLE, then every server requires a separate election
             * port.
             */
            if (electionAlg != 0) {
                for (QuorumServer s : servers.values()) {
                    if (s.electionAddr == null)
                        throw new IllegalArgumentException(
                                "Missing election port for server: " + s.id);
                }
            }

            /*
             * Default of quorum config is majority
             */

            /*
             * The default QuorumVerifier is QuorumMaj
             */

            LOG.info("Defaulting to majority quorums");
            quorumVerifier = new M2mQuorumMaj(servers.size());

            m2mDataBase = new DSDatabaseImpl(cleaned, keySpace, table, node);
            File myIdFile = new File(dataDir, "myid");
            if (!myIdFile.exists()) {
                throw new IllegalArgumentException(myIdFile.toString()
                        + " file is missing");
            }
            BufferedReader br = new BufferedReader(new FileReader(myIdFile));
            String myIdString;
            try {
                myIdString = br.readLine();
            } finally {
                br.close();
            }
            try {
                serverId = Long.parseLong(myIdString);
                MDC.put("myid", myIdString);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("serverid " + myIdString
                        + " is not a number");
            }

            myIp = servers.get(serverId).addr.getAddress().getHostAddress();

        }
        setAllReplicationServers();
    }

    /**
     * 设置不同zab集群的server
     *
     * @throws LoadBalanceException
     */
    public void setAllReplicationServers() throws LoadBalanceException {
        networkPool = new LoadBalanceConsistentHash();
        // TODO 将allServerString这里修改为函数式
        for (M2mAddressToId m2mAddressToId : addressToSid) {
            Long sid = m2mAddressToId.getSid();
            Integer port = sidToClientPort.get(sid);
            String address = m2mAddressToId.getAddress();
            allServerStrings.add(address + ":" + port);
            allServerToSids.put(address + ":" + port, sid);
        }
        if (numOfVirtualNode != null) {
            networkPool.setNumOfVirtualNode(numOfVirtualNode);
        }
        if (replication_factor != null) {
            networkPool.setReplicationFactor(replication_factor);
        }

        networkPool.setAllServers(allServerStrings);
        List<String> responseServers = networkPool.getReponseServers(myIp + ":"
                + zabClientPort);
        for (String responseServer : responseServers) {
            List<String> replicationServer = networkPool
                    .getReplicationServer(responseServer);
            HashMap<Long, QuorumServer> map = new HashMap<>();

            for (String server : replicationServer) {
                Long sid = allServerToSids.get(server);
                QuorumServer quorumServer = servers.get(sid);
                String address = quorumServer.addr.getAddress()
                        .getHostAddress();

                Integer firstPort = quorumServer.addr.getPort();
                Integer secondPort = quorumServer.electionAddr.getPort();
                int distance = Integer.valueOf(""
                        + networkPool.getServerResponseForAnthorSerer(
                        responseServer, server));
                InetSocketAddress firstInetSocketAddress = new InetSocketAddress(
                        address, firstPort - distance);
                InetSocketAddress secondInetSocketAddress = new InetSocketAddress(
                        address, secondPort - distance);
                QuorumServer myQuorumServer = new QuorumServer(sid,
                        firstInetSocketAddress, secondInetSocketAddress);
                map.put(sid, myQuorumServer);
            }
            quorumServersList.add(map);
        }

        m2mQuorumServers.setQuorumsList(quorumServersList);
        m2mQuorumServers.setServers(responseServers);

    }

    public InetSocketAddress getClientPortAddress() {
        return clientPortAddress;
    }

    public String getDataDir() {
        return dataDir;
    }

    public String getDataLogDir() {
        return dataLogDir;
    }

    public int getTickTime() {
        return tickTime;
    }

    public int getMaxClientCnxns() {
        return maxClientCnxns;
    }

    public int getMinSessionTimeout() {
        return minSessionTimeout;
    }

    public int getMaxSessionTimeout() {
        return maxSessionTimeout;
    }

    public int getInitLimit() {
        return initLimit;
    }

    public int getSyncLimit() {
        return syncLimit;
    }

    public int getElectionAlg() {
        return electionAlg;
    }

    public int getElectionPort() {
        return electionPort;
    }

    public int getSnapRetainCount() {
        return snapRetainCount;
    }

    public boolean getSyncEnabled() {
        return syncEnabled;
    }

    public M2mQuorumVerifier getQuorumVerifier() {
        return quorumVerifier;
    }

    public Map<Long, QuorumServer> getServers() {
        return Collections.unmodifiableMap(servers);
    }

    public long getServerId() {
        return serverId;
    }

    public boolean isDistributed() {
        return servers.size() > 1;
    }

    public String getMyIp() {
        return myIp;
    }

    public void setMyIp(String myIp) {
        this.myIp = myIp;
    }

    public String getZooKeeperServer() {
        return zooKeeperServer;
    }

    public void setZooKeeperServer(String zooKeeperServer) {
        this.zooKeeperServer = zooKeeperServer;
    }

    public Integer getReplication_factor() {
        return replication_factor;
    }

    public void setReplication_factor(Integer replication_factor) {
        this.replication_factor = replication_factor;
    }

    public M2mQuorumServer getM2mQuorumServers() {
        return m2mQuorumServers;
    }

    public void setM2mQuorumServers(M2mQuorumServer m2mQuorumServers) {
        this.m2mQuorumServers = m2mQuorumServers;
    }

    public LoadBalanceConsistentHash getNetworkPool() {
        return networkPool;
    }

    public Integer getWebPort() {
        return webPort;
    }

    public void setWebPort(Integer webPort) {
        this.webPort = webPort;
    }

    @SuppressWarnings("serial")
    public static class ConfigException extends Exception {
        public ConfigException(String msg) {
            super(msg);
        }

        public ConfigException(String msg, Exception e) {
            super(msg, e);
        }
    }

}
