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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lab.mars.ds.loadbalance.RangeDO;
import lab.mars.ds.persistence.DSDatabaseImpl;

import org.lab.mars.onem2m.server.DSDatabase;
import org.lab.mars.onem2m.server.ServerCnxnFactory;
import org.lab.mars.onem2m.server.ZooKeeperServer;
import org.lab.mars.onem2m.server.quorum.flexible.M2mQuorumMaj;
import org.lab.mars.onem2m.server.quorum.flexible.M2mQuorumVerifier;
import org.lab.mars.onem2m.server.util.ZxidUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class M2mQuorumPeer extends Thread implements QuorumStats.Provider {
    /**
     * The syncEnabled can also be set via a system property.
     */
    public static final String SYNC_ENABLED = "zookeeper.observer.syncEnabled";
    public static final String ACCEPTED_EPOCH_FILENAME = "acceptedEpoch";
    public static final String UPDATING_EPOCH_FILENAME = "updatingEpoch";
    private static final Logger LOG = LoggerFactory
            .getLogger(M2mQuorumPeer.class);
    /*
     * Record leader election time
     */
    public long start_fle, end_fle;
    public M2mFollower follower;
    public M2mLeader leader;
    /**
     * The servers that make up the cluster
     */
    protected Map<Long, QuorumServer> quorumPeers;
    /**
     * The number of milliseconds of each tick
     */
    protected int tickTime;

    /*
     * To enable observers to have no identifier, we need a generic identifier
     * at least for QuorumCnxManager. We use the following constant to as the
     * value of such a generic identifier.
     */
    /**
     * Minimum number of milliseconds to allow for session timeout. A value of
     * -1 indicates unset, use default.
     */
    protected int minSessionTimeout = -1;
    /**
     * Maximum number of milliseconds to allow for session timeout. A value of
     * -1 indicates unset, use default.
     */
    protected int maxSessionTimeout = -1;
    /**
     * The number of ticks that the initial synchronization phase can take
     */
    protected int initLimit;
    /**
     * The number of ticks that can pass between sending a request and getting
     * an acknowledgment
     */
    protected int syncLimit;
    /**
     * Enables/Disables sync request processor. This option is enabled by
     * default and is to be used with observers.
     */
    protected boolean syncEnabled = true;
    /**
     * The current tick
     */
    protected volatile int tick;
    M2mQuorumCnxManager qcm;
    DSDatabaseImpl m2mDataBase;
    String myIp;
    volatile boolean running = true;
    DatagramSocket udpSocket;
    M2mElection electionAlg;
    ServerCnxnFactory cnxnFactory;

    M2mQuorumPeerMain quorumPeerMain;
    private DSDatabase zkDb;
    private boolean isStart = false;
    /**
     * 它所负责接管数据的 server对应的ip
     */
    private String handleIp;
    /**
     * QuorumVerifier implementation; default (majority).
     */

    private M2mQuorumVerifier quorumConfig;
    /**
     * My id
     */
    private long myid;
    /**
     * This is who I think the leader currently is.
     */
    volatile private M2mVote currentVote;
    /**
     * ... and its counterpart for backward compatibility
     */
    volatile private M2mVote bcVote;
    /**
     * Whether or not to listen on all IPs for the two quorum ports (broadcast
     * and fast leader election).
     */

    private ServerState state = ServerState.LOOKING;
    /**
     * 我自己的本地addr
     */
    private InetSocketAddress myQuorumAddr;
    private int electionType;
    private long acceptedEpoch = -1;
    private long currentEpoch = -1;

    private String dataLogDir;

    private List<RangeDO> rangeDOs;

    public M2mQuorumPeer() {
        super("QuorumPeer");
    }

    public M2mQuorumPeer(Boolean isStart) {
        this();
        this.isStart = isStart;
    }

    /**
     * For backward compatibility purposes, we instantiate QuorumMaj by default.
     */

    public M2mQuorumPeer(Map<Long, QuorumServer> quorumPeers, File dataDir,
            File dataLogDir, int electionType, long myid, int tickTime,
            int initLimit, int syncLimit, ServerCnxnFactory cnxnFactory)
            throws IOException {
        this(quorumPeers, dataDir, dataLogDir, electionType, myid, tickTime,
                initLimit, syncLimit, false, cnxnFactory, new M2mQuorumMaj(
                        countParticipants(quorumPeers)));
    }

    public M2mQuorumPeer(Map<Long, QuorumServer> quorumPeers, File dataDir,
            File dataLogDir, int electionType, long myid, int tickTime,
            int initLimit, int syncLimit, boolean quorumListenOnAllIPs,
            ServerCnxnFactory cnxnFactory, M2mQuorumVerifier quorumConfig)
            throws IOException {
        this();
        this.cnxnFactory = cnxnFactory;
        this.quorumPeers = quorumPeers;
        this.electionType = electionType;
        this.myid = myid;
        this.tickTime = tickTime;
        this.initLimit = initLimit;
        this.syncLimit = syncLimit;
        this.zkDb = new DSDatabase(m2mDataBase);
        this.m2mDataBase = new DSDatabaseImpl();
        if (quorumConfig == null)
            this.quorumConfig = new M2mQuorumMaj(countParticipants(quorumPeers));
        else
            this.quorumConfig = quorumConfig;
    }

    /**
     * This constructor is only used by the existing unit test code. It defaults
     * to FileLogProvider persistence provider.
     */
    public M2mQuorumPeer(Map<Long, QuorumServer> quorumPeers, File snapDir,
            File logDir, int clientPort, int electionAlg, long myid,
            int tickTime, int initLimit, int syncLimit) throws IOException {
        this(quorumPeers, snapDir, logDir, electionAlg, myid, tickTime,
                initLimit, syncLimit, false, ServerCnxnFactory.createFactory(
                        new InetSocketAddress(clientPort), -1),
                new M2mQuorumMaj(countParticipants(quorumPeers)));
    }

    /**
     * This constructor is only used by the existing unit test code. It defaults
     * to FileLogProvider persistence provider.
     */
    public M2mQuorumPeer(Map<Long, QuorumServer> quorumPeers, File snapDir,
            File logDir, int clientPort, int electionAlg, long myid,
            int tickTime, int initLimit, int syncLimit,
            M2mQuorumVerifier quorumConfig) throws IOException {
        this(quorumPeers, snapDir, logDir, electionAlg, myid, tickTime,
                initLimit, syncLimit, false, ServerCnxnFactory.createFactory(
                        new InetSocketAddress(clientPort), -1), quorumConfig);
    }

    /**
     * Count the number of nodes in the map that could be followers.
     *
     * @param peers
     * @return The number of followers in the map
     */
    protected static int countParticipants(Map<Long, QuorumServer> peers) {
        return peers.size();
    }

    public int getQuorumSize() {
        return getVotingView().size();
    }

    /**
     * get the id of this quorum peer.
     */
    public long getId() {
        return myid;
    }

    public synchronized M2mVote getCurrentVote() {
        return currentVote;
    }

    /**
     * @param v
     */
    public synchronized void setCurrentVote(M2mVote v) {
        currentVote = v;
    }

    synchronized M2mVote getBCVote() {
        if (bcVote == null) {
            return currentVote;
        } else {
            return bcVote;
        }
    }

    synchronized void setBCVote(M2mVote v) {
        bcVote = v;
    }

    public synchronized ServerState getPeerState() {
        return state;
    }

    public synchronized void setPeerState(ServerState newState) {
        state = newState;
    }

    public InetSocketAddress getQuorumAddress() {
        return myQuorumAddr;
    }

    @Override
    public synchronized void start() {
        if (isStart == true) {
            try {
                cnxnFactory.start();
            } catch (Exception e) {
                LOG.error("zookeeper monitor start errror:{}", e);
            }
        }
        loadDataBase();

        startLeaderElection();
        super.start();
    }

    /**
     * 将Zxid写入文件中
     */
    private void loadDataBase() {
        ;
        try {
            zkDb.setRangeDOs(rangeDOs);

            zkDb.loadDataBase();

            // load the epochs
            long lastProcessedZxid = zkDb.getLastProcessedZxid();
            // System.exit(1);
            long epochOfZxid = ZxidUtils.getEpochFromZxid(lastProcessedZxid);

            System.out.println("epoch of zxid::::" + epochOfZxid);
            currentEpoch = epochOfZxid;
            acceptedEpoch = epochOfZxid;
        } catch (IOException ie) {
            LOG.error("Unable to load database on disk", ie);
            throw new RuntimeException("Unable to run quorum server ", ie);
        }

    }

    synchronized public void startLeaderElection() {
        try {
            currentVote = new M2mVote(myid, getLastLoggedZxid(),
                    getCurrentEpoch());
        } catch (IOException e) {
            RuntimeException re = new RuntimeException(e.getMessage());
            re.setStackTrace(e.getStackTrace());
            throw re;
        }
        for (QuorumServer p : getView().values()) {
            if (p.id == myid) {
                myQuorumAddr = p.addr;
                break;
            }
        }
        if (myQuorumAddr == null) {
            throw new RuntimeException("My id " + myid
                    + " not in the peer list");
        }
        this.electionAlg = createElectionAlgorithm(electionType);
    }

    /**
     * returns the highest zxid that this host has seen
     *
     * @return the highest zxid for this host
     */
    public long getLastLoggedZxid() {
        if (!zkDb.isInitialized()) {
            loadDataBase();
        }
        return zkDb.getLastProcessedZxid();
    }

    protected M2mFollower makeFollower() throws IOException {
        return new M2mFollower(this, new M2mFollowerZooKeeperServer(this,
                this.zkDb));
    }

    protected M2mLeader makeLeader() throws IOException {
        return new M2mLeader(this,
                new M2mLeaderZooKeeperServer(this, this.zkDb));
    }

    protected M2mElection createElectionAlgorithm(int electionAlgorithm) {
        M2mElection le = null;
        switch (electionAlgorithm) {
        case 0:
            le = new M2mLeaderElection(this);
            break;

        case 3:
            qcm = new M2mQuorumCnxManager(this);
            M2mQuorumCnxManager.Listener listener = qcm.listener;
            if (listener != null) {
                listener.start();
                le = new M2mFastLeaderElection(this, qcm);
            } else {
                LOG.error("Null listener when initializing cnx manager");
            }
            break;
        default:
            assert false;
        }
        return le;
    }

    protected M2mElection makeLEStrategy() {
        LOG.debug("Initializing leader election protocol...");
        if (getElectionType() == 0) {
            electionAlg = new M2mLeaderElection(this);
        }
        return electionAlg;
    }

    synchronized protected void setLeader(M2mLeader newLeader) {
        leader = newLeader;
    }

    synchronized protected void setFollower(M2mFollower newFollower) {
        follower = newFollower;
    }

    synchronized public ZooKeeperServer getActiveServer() {
        if (leader != null)
            return leader.zk;
        else if (follower != null)
            return follower.zk;
        return null;
    }

    @Override
    public void run() {
        setName("QuorumPeer" + "[myid=" + getId() + "]"
                + cnxnFactory.getLocalAddress());

        LOG.debug("Starting quorum peer");

        try {
            /*
             * Main loop
             */
            while (running) {
                switch (getPeerState()) {
                case LOOKING:
                    LOG.info("LOOKING");
                    try {
                        setBCVote(null);
                        setCurrentVote(makeLEStrategy().lookForLeader());
                    } catch (Exception e) {
                        LOG.warn("Unexpected exception", e);
                        setPeerState(ServerState.LOOKING);
                    }

                    break;
                case FOLLOWING:
                    try {
                        LOG.info("FOLLOWING");
                        setFollower(makeFollower());
                        follower.followLeader();

                    } catch (Exception e) {
                        LOG.warn("Unexpected exception", e);
                    } finally {
                        follower.shutdown();
                        setFollower(null);
                        setPeerState(ServerState.LOOKING);
                    }
                    break;
                case LEADING:
                    LOG.info("LEADING");
                    try {
                        setLeader(makeLeader());
                        leader.lead();
                        setLeader(null);
                    } catch (Exception e) {
                        LOG.warn("Unexpected exception", e);
                    } finally {
                        if (leader != null) {
                            leader.shutdown("Forcing shutdown");
                            setLeader(null);
                        }
                        setPeerState(ServerState.LOOKING);
                    }
                    break;

                }
            }
        } finally {
            LOG.warn("QuorumPeer main thread exited");

        }
    }

    public void shutdown() {
        running = false;
        if (leader != null) {
            leader.shutdown("quorum Peer shutdown");
        }
        if (follower != null) {
            follower.shutdown();
        }
        cnxnFactory.shutdown();
        if (udpSocket != null) {
            udpSocket.close();
        }

        if (getElectionAlg() != null) {
            this.interrupt();
            getElectionAlg().shutdown();
        }
        try {
            zkDb.close();
        } catch (IOException ie) {
            LOG.warn("Error closing logs ", ie);
        }
    }

    /**
     * A 'view' is a node's current opinion of the membership of the entire
     * ensemble.
     */
    public Map<Long, QuorumServer> getView() {
        return Collections.unmodifiableMap(this.quorumPeers);
    }

    /**
     * Observers are not contained in this view, only nodes with
     * PeerType=PARTICIPANT.
     */
    public Map<Long, QuorumServer> getVotingView() {
        Map<Long, QuorumServer> view = getView();
        return view;
    }

    /**
     * Returns only observers, no followers.
     */
    public Map<Long, QuorumServer> getObservingView() {
        Map<Long, QuorumServer> ret = new HashMap<Long, QuorumServer>();
        return ret;
    }

    /**
     * Check if a node is in the current view. With static membership, the
     * result of this check will never change; only when dynamic membership is
     * introduced will this be more useful.
     */
    public boolean viewContains(Long sid) {
        return this.quorumPeers.containsKey(sid);
    }

    /**
     * Only used by QuorumStats at the moment
     */
    public String[] getQuorumPeers() {
        List<String> l = new ArrayList<String>();
        synchronized (this) {
            if (leader != null) {
                for (M2mLearnerHandler fh : leader.getLearners()) {
                    if (fh.getSocket() != null) {
                        String s = fh.getSocket().getRemoteSocketAddress()
                                .toString();
                        if (leader.isLearnerSynced(fh))
                            s += "*";
                        l.add(s);
                    }
                }
            } else if (follower != null) {
                l.add(follower.sock.getRemoteSocketAddress().toString());
            }
        }
        return l.toArray(new String[0]);
    }

    public void setQuorumPeers(Map<Long, QuorumServer> quorumPeers) {
        this.quorumPeers = quorumPeers;
    }

    public String getServerState() {
        switch (getPeerState()) {
        case LOOKING:
            return QuorumStats.Provider.LOOKING_STATE;
        case LEADING:
            return QuorumStats.Provider.LEADING_STATE;
        case FOLLOWING:
            return QuorumStats.Provider.FOLLOWING_STATE;
        }
        return QuorumStats.Provider.UNKNOWN_STATE;
    }

    /**
     * get the id of this quorum peer.
     */
    public long getMyid() {
        return myid;
    }

    /**
     * set the id of this quorum peer.
     */
    public void setMyid(long myid) {
        this.myid = myid;
    }

    /**
     * Get the number of milliseconds of each tick
     */
    public int getTickTime() {
        return tickTime;
    }

    /**
     * Set the number of milliseconds of each tick
     */
    public void setTickTime(int tickTime) {
        LOG.info("tickTime set to " + tickTime);
        this.tickTime = tickTime;
    }

    /**
     * Maximum number of connections allowed from particular host (ip)
     */
    public int getMaxClientCnxnsPerHost() {
        ServerCnxnFactory fac = getCnxnFactory();
        if (fac == null) {
            return -1;
        }
        return fac.getMaxClientCnxnsPerHost();
    }

    /**
     * minimum session timeout in milliseconds
     */
    public int getMinSessionTimeout() {
        return minSessionTimeout == -1 ? tickTime * 2 : minSessionTimeout;
    }

    /**
     * minimum session timeout in milliseconds
     */
    public void setMinSessionTimeout(int min) {
        LOG.info("minSessionTimeout set to " + min);
        this.minSessionTimeout = min;
    }

    /**
     * maximum session timeout in milliseconds
     */
    public int getMaxSessionTimeout() {
        return maxSessionTimeout == -1 ? tickTime * 20 : maxSessionTimeout;
    }

    /**
     * minimum session timeout in milliseconds
     */
    public void setMaxSessionTimeout(int max) {
        LOG.info("maxSessionTimeout set to " + max);
        this.maxSessionTimeout = max;
    }

    /**
     * Get the number of ticks that the initial synchronization phase can take
     */
    public int getInitLimit() {
        return initLimit;
    }

    /**
     * Set the number of ticks that the initial synchronization phase can take
     */
    public void setInitLimit(int initLimit) {
        LOG.info("initLimit set to " + initLimit);
        this.initLimit = initLimit;
    }

    /**
     * Get the current tick
     */
    public int getTick() {
        return tick;
    }

    /**
     * Return QuorumVerifier object
     */

    public M2mQuorumVerifier getQuorumVerifier() {
        return quorumConfig;

    }

    public void setQuorumVerifier(M2mQuorumVerifier quorumConfig) {
        this.quorumConfig = quorumConfig;
    }

    /**
     * Get an instance of LeaderElection
     */

    public M2mElection getElectionAlg() {
        return electionAlg;
    }

    /**
     * Get the synclimit
     */
    public int getSyncLimit() {
        return syncLimit;
    }

    /**
     * Set the synclimit
     */
    public void setSyncLimit(int syncLimit) {
        this.syncLimit = syncLimit;
    }

    /**
     * Return syncEnabled.
     *
     * @return
     */
    public boolean getSyncEnabled() {
        if (System.getProperty(SYNC_ENABLED) != null) {
            LOG.info(SYNC_ENABLED + "=" + Boolean.getBoolean(SYNC_ENABLED));
            return Boolean.getBoolean(SYNC_ENABLED);
        } else {
            return syncEnabled;
        }
    }

    /**
     * Set syncEnabled.
     *
     * @param syncEnabled
     */
    public void setSyncEnabled(boolean syncEnabled) {
        this.syncEnabled = syncEnabled;
    }

    /**
     * Gets the election type
     */
    public int getElectionType() {
        return electionType;
    }

    /**
     * Sets the election type
     */
    public void setElectionType(int electionType) {
        this.electionType = electionType;
    }

    public ServerCnxnFactory getCnxnFactory() {
        return cnxnFactory;
    }

    public void setCnxnFactory(ServerCnxnFactory cnxnFactory) {
        this.cnxnFactory = cnxnFactory;
    }

    public int getClientPort() {
        return cnxnFactory.getLocalPort();
    }

    public void setClientPort(Integer clientPort) {
    }

    /**
     * set zk database for this node
     *
     * @param database
     */
    public void setZKDatabase(DSDatabase database) {
        this.zkDb = database;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    /**
     * get reference to QuorumCnxManager
     */
    public M2mQuorumCnxManager getQuorumCnxManager() {
        return qcm;
    }

    /**
     * 从特定文件读取zxid
     *
     * @param name
     * @return
     * @throws IOException
     */
    private long readLongFromFile(String name) throws IOException {
        File file = new File(dataLogDir, name);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line = "";
        try {
            line = br.readLine();
            return Long.parseLong(line);
        } catch (NumberFormatException e) {
            throw new IOException("Found " + line + " in " + file);
        } finally {
            br.close();
        }
    }

    public long getCurrentEpoch() throws IOException {
        return currentEpoch;
    }

    public void setCurrentEpoch(long e) throws IOException {
        currentEpoch = e;
    }

    public long getAcceptedEpoch() throws IOException {
        if (acceptedEpoch == -1) {
            acceptedEpoch = readLongFromFile(ACCEPTED_EPOCH_FILENAME);
        }
        return acceptedEpoch;
    }

    /**
     * 不用再向文件里面写入epoch
     *
     * @param e
     * @throws IOException
     */
    public void setAcceptedEpoch(long e) throws IOException {
        acceptedEpoch = e;
    }

    /**
     * Updates leader election info to avoid inconsistencies when a new server
     * tries to join the ensemble. See ZOOKEEPER-1732 for more info.
     */
    protected void updateElectionVote(long newEpoch) {
        M2mVote currentVote = getCurrentVote();
        setBCVote(currentVote);
        if (currentVote != null) {
            setCurrentVote(new M2mVote(currentVote.getId(),
                    currentVote.getZxid(), currentVote.getElectionEpoch(),
                    newEpoch, currentVote.getState()));
        }
    }

    public void setMyIp(String myIp) {
        this.myIp = myIp;
    }

    public String getHandleIp() {
        return handleIp;
    }

    public void setHandleIp(String handleIp) {
        this.handleIp = handleIp;
    }

    public void setQuorumPeerMain(M2mQuorumPeerMain quorumPeerMain) {
        this.quorumPeerMain = quorumPeerMain;
    }

    public void setDataLogDir(String dataLogDir) {
        this.dataLogDir = dataLogDir;
    }

    public void setRangeDOs(List<RangeDO> rangeDOs) {
        this.rangeDOs = rangeDOs;
    }

    public enum ServerState {
        LOOKING, FOLLOWING, LEADING;
    }

    public static class QuorumServer {
        public InetSocketAddress addr;
        public InetSocketAddress electionAddr;
        public long id;

        public QuorumServer(long id, InetSocketAddress addr,
                InetSocketAddress electionAddr) {
            this.id = id;
            this.addr = addr;
            this.electionAddr = electionAddr;
        }

        public QuorumServer(long id, InetSocketAddress addr) {
            this.id = id;
            this.addr = addr;
            this.electionAddr = null;
        }
    }
}
