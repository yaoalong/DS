package org.apache.zookeeper.server.quorum;

import java.io.File;
import java.io.IOException;

import javax.management.JMException;

import org.apache.zookeeper.jmx.ManagedUtil;
import org.apache.zookeeper.server.DatadirCleanupManager;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ZKDatabase;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * <h2>Configuration file</h2>
 *
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
 *
 */
public class QuorumPeerMain {
    private static final Logger LOG = LoggerFactory
            .getLogger(QuorumPeerMain.class);

    private static final String USAGE = "Usage: QuorumPeerMain configfile";

    protected QuorumPeer quorumPeer;

    /**
     * To start the replicated server specify the configuration file name on the
     * command line.
     * 
     * @param args
     *            path to the configfile
     */
    public static void main(String[] args) {
        QuorumPeerMain quorumPeerMain = new QuorumPeerMain();
        try {
            quorumPeerMain.parse(args);
        } catch (ConfigException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    QuorumPeerConfig config;

    public void parse(String args[]) throws ConfigException {
        config = new QuorumPeerConfig();
        if (args.length == 1) {
            config.parse(args[0]);
        }
    }

    public void runFromConfig(QuorumPeerConfig config) throws IOException {
        // Start and schedule the the purge task
        DatadirCleanupManager purgeMgr = new DatadirCleanupManager(
                config.getDataDir(), config.getDataLogDir(),
                config.getSnapRetainCount(), config.getPurgeInterval());
        purgeMgr.start();
        try {
            ManagedUtil.registerLog4jMBeans();
        } catch (JMException e) {
            LOG.warn("Unable to register log4j JMX control", e);
        }

        LOG.info("Starting quorum peer");
        try {
            ServerCnxnFactory cnxnFactory = ServerCnxnFactory.createFactory();
            cnxnFactory.configure(config.getClientPortAddress(),
                    config.getMaxClientCnxns());

            quorumPeer = new QuorumPeer();
            quorumPeer.setClientPortAddress(config.getClientPortAddress());
            quorumPeer.setTxnFactory(new FileTxnSnapLog(new File(config
                    .getDataLogDir()), new File(config.getDataDir())));
            quorumPeer.setQuorumPeers(config.getServers());
            quorumPeer.setElectionType(config.getElectionAlg());
            quorumPeer.setMyid(config.getServerId());
            quorumPeer.setTickTime(config.getTickTime());
            quorumPeer.setMinSessionTimeout(config.getMinSessionTimeout());
            quorumPeer.setMaxSessionTimeout(config.getMaxSessionTimeout());
            quorumPeer.setInitLimit(config.getInitLimit());
            quorumPeer.setSyncLimit(config.getSyncLimit());
            quorumPeer.setQuorumVerifier(config.getQuorumVerifier());
            quorumPeer.setCnxnFactory(cnxnFactory);
            quorumPeer
                    .setZKDatabase(new ZKDatabase(quorumPeer.getTxnFactory()));
            quorumPeer.setLearnerType(config.getPeerType());
            quorumPeer.setSyncEnabled(config.getSyncEnabled());
            quorumPeer
                    .setQuorumListenOnAllIPs(config.getQuorumListenOnAllIPs());

            quorumPeer.start();
            quorumPeer.join();
        } catch (InterruptedException e) {
            // warn, but generally this is ok
            LOG.warn("Quorum Peer interrupted", e);
        }
    }

    public QuorumPeerConfig getConfig() {
        return config;
    }

    public void setConfig(QuorumPeerConfig config) {
        this.config = config;
    }

}
