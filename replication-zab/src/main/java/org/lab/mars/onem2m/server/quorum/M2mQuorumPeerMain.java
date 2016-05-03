package org.lab.mars.onem2m.server.quorum;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import lab.mars.ds.collaboration.ZKRegisterAndMonitorService;
import lab.mars.ds.ds.persistence.FileTxnLog;
import lab.mars.ds.loadbalance.LoadBalanceException;
import lab.mars.ds.loadbalance.impl.NetworkPool;
import lab.mars.ds.util.Statistics;
import lab.mars.ds.web.network.WebTcpServer;

import org.lab.mars.onem2m.OneM2m;
import org.lab.mars.onem2m.proto.M2mPacket;
import org.lab.mars.onem2m.server.DSDatabase;
import org.lab.mars.onem2m.server.NettyServerCnxnFactory;
import org.lab.mars.onem2m.server.quorum.M2mQuorumPeer.QuorumServer;
import org.lab.mars.onem2m.server.quorum.QuorumPeerConfig.ConfigException;
import org.lab.mars.onem2m.server.quorum.flexible.M2mQuorumMaj;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class M2mQuorumPeerMain extends Thread {
    private static final Logger LOG = LoggerFactory
            .getLogger(M2mQuorumPeerMain.class);

    private static final String USAGE = "Usage: QuorumPeerMain configfile";
    public volatile boolean isStarted;
    private M2mHandler m2mHandler;
    private OneM2m oneM2m;
    private volatile Integer zabClientPort;
    private volatile String myAddress;

    private String[] configFile;

    /**
     * To start the replicated server specify the configuration file name on the
     * command line.
     *
     * @param args
     *            path to the configfile
     */
    public static void main(String[] args) {
        M2mQuorumPeerMain main = new M2mQuorumPeerMain();
        try {
            main.bindHandler(new CRUDM2mHandler());
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
            IOException, LoadBalanceException {
        QuorumPeerConfig config = new QuorumPeerConfig();
        if (args.length == 1) {
            config.parse(args[0]);
        }
        if (args.length == 1 && config.servers.size() > 0) {
            runFromConfig(config, args);
        } else {
            LOG.error("Either no config or no quorum defined in config, System exit");
            System.exit(0);

        }
    }

    public void runFromConfig(QuorumPeerConfig config, String[] args)
            throws IOException, LoadBalanceException {
        zabClientPort = config.zabClientPort;
        myAddress = config.getMyIp();
        LOG.info("Starting quorum peer");
        try {
            NetworkPool networkPool = new NetworkPool();
            Statistics statistics=new Statistics();
            networkPool.setReplicationFactor(config.replication_factor);
            networkPool.setNumOfVirtualNode(config.numOfVirtualNode);
            networkPool.setAllServers(config.allServerStrings);

            NettyServerCnxnFactory cnxnFactory = new NettyServerCnxnFactory(
                    this);
            networkPool.setReplicationFactor(config.replication_factor);
            cnxnFactory.setNetworkPool(networkPool);
            cnxnFactory.configure(config.zabClientPort, 5, m2mHandler,
                    config.numberOfConnections);
            cnxnFactory.setMyIp(config.getMyIp());
            cnxnFactory.setReplicationFactory(config.getReplication_factor());// 设置复制因子
            ZKRegisterAndMonitorService zkRegisterAndMonitorService = new ZKRegisterAndMonitorService();
            zkRegisterAndMonitorService.register(config.zooKeeperServerString,
                    myAddress + ":" + zabClientPort, networkPool);
            List<M2mQuorumPeer> quorumPeers = new ArrayList<M2mQuorumPeer>();

            for (int i = 0; i < config.quorumServersList.size(); i++) {
                M2mQuorumPeer quorumPeer;
                M2mQuorumServer m2mQuorumServer = config.getM2mQuorumServers();
                HashMap<Long, QuorumServer> servers = m2mQuorumServer
                        .getQuorumsList().get(i);

                if (i == 0) {
                    quorumPeer = new M2mQuorumPeer(true);
                } else {
                    quorumPeer = new M2mQuorumPeer();
                }
                quorumPeer.setHandleIp(m2mQuorumServer.getServers().get(i));
                quorumPeer.setQuorumVerifier(new M2mQuorumMaj(servers.size()));
                quorumPeer.setQuorumPeers(servers);// 设置对应的服务器信息
                quorumPeer.setFileTxnLog(new FileTxnLog(new File(config
                        .getDataDir() + "/log")));
                quorumPeer.setElectionType(config.getElectionAlg());
                quorumPeer.setCnxnFactory(cnxnFactory);

                quorumPeer.setZKDatabase(new DSDatabase(config.m2mDataBase,
                        quorumPeer.getFileTxnLog()));
                quorumPeer.setMyid(config.getServerId());
                quorumPeer.setTickTime(config.getTickTime());
                quorumPeer.setMinSessionTimeout(config.getMinSessionTimeout());
                quorumPeer.setMaxSessionTimeout(config.getMaxSessionTimeout());
                quorumPeer.setInitLimit(config.getInitLimit());
                quorumPeer.setSyncLimit(config.getSyncLimit());
                quorumPeer.setSyncEnabled(config.getSyncEnabled());
                quorumPeer.setDataLogDir(config.getDataLogDir());
                quorumPeer.setQuorumPeerMain(this);
                quorumPeer.setRangeDOs(networkPool.getRanges(quorumPeer
                        .getHandleIp()));

                quorumPeer.setMyIp(config.getMyIp());

                quorumPeer.start();
                quorumPeers.add(quorumPeer);

            }
            cnxnFactory.setWebServer(config.webServers);
            cnxnFactory.setMyWebIp(config.webServers.get(config.serverId));
            WebTcpServer webTcpServer = new WebTcpServer(cnxnFactory);
            webTcpServer.bind(config.getMyIp(),
                    config.sidAndWebPort.get(config.serverId));
            for (M2mQuorumPeer quorumPeer : quorumPeers) {
                quorumPeer.join();
            }

        } catch (InterruptedException e) {
            LOG.warn("Quorum Peer interrupted", e);
        }
    }

    @Override
    public void run() {
        try {
            try {
                initializeAndRun(configFile);
            } catch (LoadBalanceException e) {
                e.printStackTrace();
            }
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
            while (myAddress == null && zabClientPort == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            oneM2m = new OneM2m(myAddress, zabClientPort);

        }

        oneM2m.send(m2mPacket);
        return m2mPacket;
    }

    public void setConfigFile(String[] configFile) {
        this.configFile = configFile;
    }
}
