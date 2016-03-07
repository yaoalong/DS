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

import lab.mars.ds.loadbalance.impl.NetworkPool;
import lab.mars.ds.persistence.DSDatabaseImpl;
import lab.mars.ds.register.ZooKeeperRegister;
import org.lab.mars.onem2m.OneM2m;
import org.lab.mars.onem2m.proto.M2mPacket;
import org.lab.mars.onem2m.server.NettyServerCnxnFactory;
import org.lab.mars.onem2m.server.ZKDatabase;
import org.lab.mars.onem2m.server.quorum.M2mQuorumPeer.QuorumServer;
import org.lab.mars.onem2m.server.quorum.QuorumPeerConfig.ConfigException;
import org.lab.mars.onem2m.server.quorum.flexible.M2mQuorumMaj;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * <h2>Configuration file</h2>
 * <p>
 * When the main() method of this class is used to start the program, the first
 * argument is used as a path to the config file, which will be used to obtain
 * configuration information. This file is a Properties file, so keys and values
 * are separated by equals (=) and the key/value pairs are separated by new
 * lines. The following is a general summary of keys used in the configuration
 * file. For full details on this see the documentation in docs/index.html
 * <ol>
 * <li>dataDir - The directory where the ZooKeeper data is stored.</li>
 * <li>dataLogDir - The directory where the ZooKeeper transaction log is stored.
 * </li>
 * <li>clientPort - The port used to communicate with clients.</li>
 * <li>tickTime - The duration of a tick in milliseconds. This is the basic unit
 * of time in ZooKeeper.</li>
 * <li>initLimit - The maximum number of ticks that a follower will wait to
 * initially synchronize with a leader.</li>
 * <li>syncLimit - The maximum number of ticks that a follower will wait for a
 * message (including heartbeats) from the leader.</li>
 * <li>server.<i>id</i> - This is the host:port[:port] that the server with the
 * given id will use for the quorum protocol.</li>
 * </ol>
 * In addition to the config file. There is a file in the data directory called
 * "myid" that contains the server id as an ASCII decimal value.
 */
public class M2mQuorumPeerMain extends Thread {
    private static final Logger LOG = LoggerFactory
            .getLogger(M2mQuorumPeerMain.class);

    private static final String USAGE = "Usage: QuorumPeerMain configfile";
    public volatile boolean isStarted;
    private M2mHandler m2mHandler;
    private OneM2m oneM2m;
    private volatile Integer webPort;
    private volatile String address;

    private String[] configFile;

    /**
     * To start the replicated server specify the configuration file name on the
     * command line.
     *
     * @param args path to the configfile
     */
    public static void main(String[] args) {
        M2mQuorumPeerMain main = new M2mQuorumPeerMain();
        try {
            main.initializeAndRun(args);
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid arguments, exiting abnormally", e);
            LOG.info(USAGE);
            System.err.println(USAGE);
            System.exit(2);
        } catch (ConfigException e) {
            LOG.error("Invalid config, exiting abnormally", e);
            System.err.println("Invalid config, exiting abnormally");
            System.exit(2);
        } catch (Exception e) {
            LOG.error("Unexpected exception, exiting abnormally", e);
            System.exit(1);
        }
        LOG.info("Exiting normally");
        System.exit(0);
    }

    protected void initializeAndRun(String[] args) throws ConfigException,
            IOException {
        QuorumPeerConfig config = new QuorumPeerConfig();
        if (args.length == 1) {
            config.parse(args[0]);
        }
        System.out.println("hha");
        if (args.length == 1 && config.servers.size() > 0) {
            runFromConfig(config, args);
        } else {
            LOG.error("Either no config or no quorum defined in config, running "
                    + " in standalone mode");
            System.exit(0);

        }
    }

    public void runFromConfig(QuorumPeerConfig config, String[] args)
            throws IOException {

        webPort = config.clientPort;
        address = config.getMyIp();
        LOG.info("Starting quorum peer");
        try {
            NetworkPool networkPool = new NetworkPool();
            networkPool.setAllServers(config.allServerStrings);
            networkPool.setFactor(config.replication_factor);
            NettyServerCnxnFactory cnxnFactory = new NettyServerCnxnFactory(
                    this);
            cnxnFactory.setNetworkPool(networkPool);
            cnxnFactory.configure(config.getClientPortAddress().getPort(), 5,
                    m2mHandler);
            cnxnFactory.setMyIp(config.getMyIp());
            cnxnFactory.setAllServers(config.allServers);
            cnxnFactory.setReplicationFactory(config.getReplication_factor());// 设置复制因子
            cnxnFactory.setTemporyAdd(config.isTemporyAdd());

            ZooKeeperRegister zooKeeperRegister = new ZooKeeperRegister();
            // TODO zooKeeperRegister.starter(args, networkPool);
            System.out.println("哈市");
            // TODO zooKeeperRegister.register(address + ":" + webPort);
            List<M2mQuorumPeer> quorumPeers = new ArrayList<M2mQuorumPeer>();
            long minValue = config.isTemporyAdd() ? 1
                    : config.replication_factor;
            for (long i = 0; i < minValue; i++) {
                M2mQuorumPeer quorumPeer;
                M2mQuorumServer m2mQuorumServer = config.getM2mQuorumServers();
                HashMap<Long, QuorumServer> servers = m2mQuorumServer
                        .getPositionToServers().get(i);

                if (i == minValue - 1) {
                    quorumPeer = new M2mQuorumPeer(true);
                } else {
                    quorumPeer = new M2mQuorumPeer();
                }
                quorumPeer.setHandleIp(m2mQuorumServer.getServers().get(
                        Integer.valueOf((i) + "")));
                quorumPeer.setQuorumVerifier(new M2mQuorumMaj(servers.size()));
                quorumPeer.setQuorumPeers(servers);// 设置对应的服务器信息
                quorumPeer.setElectionType(config.getElectionAlg());
                quorumPeer.setCnxnFactory(cnxnFactory);

                quorumPeer.setZKDatabase(new ZKDatabase(
                        config.getNetworkPool(), new DSDatabaseImpl(
                        config.m2mDataBase.isClean(),
                        config.m2mDataBase.getKeyspace(),
                        config.m2mDataBase.getTable(),
                        config.m2mDataBase.getNode()), quorumPeer
                        .getHandleIp()));
                quorumPeer.setMyid(config.getServerId());
                quorumPeer.setTickTime(config.getTickTime());
                quorumPeer.setMinSessionTimeout(config.getMinSessionTimeout());
                quorumPeer.setMaxSessionTimeout(config.getMaxSessionTimeout());
                quorumPeer.setInitLimit(config.getInitLimit());
                quorumPeer.setSyncLimit(config.getSyncLimit());
                quorumPeer.setSyncEnabled(config.getSyncEnabled());
                quorumPeer.setDataDir(config.getDataDir());
                quorumPeer.setDataLogDir(config.getDataLogDir());
                quorumPeer.setQuorumPeerMain(this);
                quorumPeer.setRangeDOs(networkPool.getRanges(config.getMyIp()
                        + ":" + config.clientPort));

                quorumPeer.setMyIp(config.getMyIp());

                quorumPeer.start();
                M2mQuorumPeerStatistics.quorums.put(m2mQuorumServer
                                .getServers().get(Integer.valueOf((i) + "")),
                        quorumPeer);
                quorumPeers.add(quorumPeer);

            }
            for (M2mQuorumPeer quorumPeer : quorumPeers) {
                quorumPeer.join();
            }

        } catch (InterruptedException e) {
            // warn, but generally this is ok
            LOG.warn("Quorum Peer interrupted", e);
        }
    }

    @Override
    public void run() {
        try {
            initializeAndRun(configFile);
        } catch (ConfigException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOG.info("Exiting normally");
        System.exit(0);
    }

    /**
     * 绑定开发者自己定义的Handler
     *
     * @param m2mHandler
     */
    public void bindHandler(M2mHandler m2mHandler) {
        this.m2mHandler = m2mHandler;
    }

    public M2mPacket request(M2mPacket m2mPacket) {
        if (!isStarted) {
            LOG.error("service is not valid!");
            return null;
        }
        if (oneM2m == null) {
            while (address == null && webPort == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            oneM2m = new OneM2m(address, webPort);

        }

        oneM2m.send(m2mPacket);
        return m2mPacket;
    }

    public void setConfigFile(String[] configFile) {
        this.configFile = configFile;
    }
}
