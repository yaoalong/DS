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

package org.lab.mars.onem2m.server.quorum;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.lab.mars.onem2m.jute.M2mBinaryOutputArchive;
import org.lab.mars.onem2m.server.M2mFinalRequestProcessor;
import org.lab.mars.onem2m.server.M2mRequest;
import org.lab.mars.onem2m.server.RequestProcessor;
import org.lab.mars.onem2m.server.quorum.flexible.M2mQuorumVerifier;
import org.lab.mars.onem2m.server.util.ZxidUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class has the control logic for the Leader.
 */
public class M2mLeader {
    private static final Logger LOG = LoggerFactory.getLogger(M2mLeader.class);

    static final private boolean nodelay = System.getProperty("leader.nodelay",
            "true").equals("true");
    static {
        LOG.info("TCP NoDelay set to: " + nodelay);
    }

    static public class Proposal {
        public M2mQuorumPacket packet;

        public HashSet<Long> ackSet = new HashSet<Long>();

        public M2mRequest m2mRequest;

        @Override
        public String toString() {
            return packet.getType() + ", " + packet.getZxid() + ", "
                    + m2mRequest;
        }
    }

    final M2mLeaderZooKeeperServer zk;
    final M2mQuorumPeer self;

    private boolean quorumFormed = false;

    // the follower acceptor thread
    LearnerCnxAcceptor cnxAcceptor;

    // list of all the followers
    private final HashSet<M2mLearnerHandler> learners = new HashSet<M2mLearnerHandler>();

    /**
     * Returns a copy of the current learner snapshot
     */
    public List<M2mLearnerHandler> getLearners() {
        synchronized (learners) {
            return new ArrayList<M2mLearnerHandler>(learners);
        }
    }

    // list of followers that are ready to follow (i.e synced with the leader)
    private final HashSet<M2mLearnerHandler> forwardingFollowers = new HashSet<M2mLearnerHandler>();

    /**
     * Returns a copy of the current forwarding follower snapshot
     */
    public List<M2mLearnerHandler> getForwardingFollowers() {
        synchronized (forwardingFollowers) {
            return new ArrayList<M2mLearnerHandler>(forwardingFollowers);
        }
    }

    private void addForwardingFollower(M2mLearnerHandler lh) {
        synchronized (forwardingFollowers) {
            forwardingFollowers.add(lh);
        }
    }

    private final HashSet<M2mLearnerHandler> observingLearners = new HashSet<M2mLearnerHandler>();

    /**
     * Returns a copy of the current observer snapshot
     */
    public List<M2mLearnerHandler> getObservingLearners() {
        synchronized (observingLearners) {
            return new ArrayList<M2mLearnerHandler>(observingLearners);
        }
    }

    // Pending sync requests. Must access under 'this' lock.
    /**
     * 根据Sid,一系列LearnerSyncRequest
     */
    private final HashMap<Long, List<LearnerSyncRequest>> pendingSyncs = new HashMap<Long, List<LearnerSyncRequest>>();

    synchronized public int getNumPendingSyncs() {
        return pendingSyncs.size();
    }

    // Follower counter
    final AtomicLong followerCounter = new AtomicLong(-1);

    /**
     * Adds peer to the leader.
     * 
     * @param learner
     *            instance of learner handle
     */
    void addLearnerHandler(M2mLearnerHandler learner) {
        synchronized (learners) {
            learners.add(learner);
        }
    }

    /**
     * Remove the learner from the learner list
     * 
     * @param peer
     */
    void removeLearnerHandler(M2mLearnerHandler peer) {
        synchronized (forwardingFollowers) {
            forwardingFollowers.remove(peer);
        }
        synchronized (learners) {
            learners.remove(peer);
        }
    }

    boolean isLearnerSynced(M2mLearnerHandler peer) {
        synchronized (forwardingFollowers) {
            return forwardingFollowers.contains(peer);
        }
    }

    ServerSocket ss;

    /**
     * server.num 中第一个端口为监听Learner的连接，而第二个则负责选举
     * 
     * @param self
     * @param zk
     * @throws IOException
     */
    M2mLeader(M2mQuorumPeer self, M2mLeaderZooKeeperServer zk)
            throws IOException {
        this.self = self;
        try {

            ss = new ServerSocket();

            ss.setReuseAddress(true);

            ss.bind(self.getQuorumAddress());

        } catch (BindException e) {

            LOG.error("Couldn't bind to " + self.getQuorumAddress(), e);

            throw e;
        }
        this.zk = zk;
    }

    /**
     * This message is for follower to expect diff
     */
    final static int DIFF = 13;

    /**
     * This is for follower to truncate its logs
     */
    final static int TRUNC = 14;

    /**
     * This is for follower to download the snapshots
     */
    final static int SNAP = 15;

    /**
     * This tells the leader that the connecting peer is actually an observer
     */
    final static int OBSERVERINFO = 16;

    /**
     * This message type is sent by the leader to indicate it's zxid and if
     * needed, its database.
     */
    final static int NEWLEADER = 10;

    /**
     * This message type is sent by a follower to pass the last zxid. This is
     * here for backward compatibility purposes.
     */
    final static int FOLLOWERINFO = 11;

    /**
     * This message type is sent by the leader to indicate that the follower is
     * now uptodate andt can start responding to clients.
     */
    final static int UPTODATE = 12;

    /**
     * This message is the first that a follower receives from the leader. It
     * has the protocol version and the epoch of the leader.
     */
    public static final int LEADERINFO = 17;

    /**
     * This message is used by the follow to ack a proposed epoch.
     */
    public static final int ACKEPOCH = 18;

    /**
     * This message type is sent to a leader to request and mutation operation.
     * The payload will consist of a request header followed by a request.
     */
    final static int REQUEST = 1;

    /**
     * This message type is sent by a leader to propose a mutation.
     */
    public final static int PROPOSAL = 2;

    /**
     * This message type is sent by a follower after it has synced a proposal.
     */
    final static int ACK = 3;

    /**
     * This message type is sent by a leader to commit a proposal and cause
     * followers to start serving the corresponding data.
     */
    final static int COMMIT = 4;

    /**
     * This message type is enchanged between follower and leader (initiated by
     * follower) to determine liveliness.
     */
    final static int PING = 5;

    /**
     * This message type is to validate a session that should be active.
     */
    final static int REVALIDATE = 6;

    /**
     * This message is a reply to a synchronize command flushing the pipe
     * between the leader and the follower.
     */
    final static int SYNC = 7;

    /**
     * This message type informs observers of a committed proposal.
     */
    final static int INFORM = 8;

    ConcurrentMap<Long, Proposal> outstandingProposals = new ConcurrentHashMap<Long, Proposal>();// 这是以<zxid,投票>

    ConcurrentLinkedQueue<Proposal> toBeApplied = new ConcurrentLinkedQueue<Proposal>();// 去应用的列表
    /**
     * 新的Leader投票 ，用来进行投票统计等作用
     */
    Proposal newLeaderProposal = new Proposal();

    class LearnerCnxAcceptor extends Thread {
        private volatile boolean stop = false;

        @Override
        public void run() {
            try {
                while (!stop) {
                    try {
                        Socket s = ss.accept();
                        // start with the initLimit, once the ack is processed
                        // in LearnerHandler switch to the syncLimit
                        s.setSoTimeout(self.tickTime * self.initLimit);
                        s.setTcpNoDelay(nodelay);
                        M2mLearnerHandler fh = new M2mLearnerHandler(s,
                                M2mLeader.this);
                        fh.start();
                    } catch (SocketException e) {
                        if (stop) {
                            LOG.info("exception while shutting down acceptor: "
                                    + e);

                            // When Leader.shutdown() calls ss.close(),
                            // the call to accept throws an exception.
                            // We catch and set stop to true.
                            stop = true;
                        } else {
                            throw e;
                        }
                    }
                }
            } catch (Exception e) {
                LOG.warn("Exception while accepting follower", e);
            }
        }

        public void halt() {
            stop = true;
        }
    }

    StateSummary leaderStateSummary;

    long epoch = -1;
    boolean waitingForNewEpoch = true;
    volatile boolean readyToStart = false;

    /**
     * This method is main function that is called to lead
     * 
     * @throws IOException
     * @throws InterruptedException
     */
    void lead() throws IOException, InterruptedException {
        self.end_fle = System.currentTimeMillis();
        LOG.info("LEADING - LEADER ELECTION TOOK - "
                + (self.end_fle - self.start_fle));
        self.start_fle = 0;
        self.end_fle = 0;

        try {
            self.tick = 0;
            zk.loadData();
            leaderStateSummary = new StateSummary(self.getCurrentEpoch(),
                    zk.getLastProcessedZxid());

            // Start thread that waits for connection requests from
            // new followers.
            cnxAcceptor = new LearnerCnxAcceptor();
            cnxAcceptor.start();

            readyToStart = true;
            long epoch = getEpochToPropose(self.getId(),
                    self.getAcceptedEpoch());
            zk.setZxid(ZxidUtils.makeZxid(epoch, 0));// 在这里的zxid已经是增长过的了

            synchronized (this) {
                lastProposed = zk.getZxid();
            }

            newLeaderProposal.packet = new M2mQuorumPacket(NEWLEADER,
                    zk.getZxid(), null);

            if ((newLeaderProposal.packet.getZxid() & 0xffffffffL) != 0) {
                LOG.info("NEWLEADER proposal has Zxid of "
                        + Long.toHexString(newLeaderProposal.packet.getZxid()));
            }

            waitForEpochAck(self.getId(), leaderStateSummary);
            self.setCurrentEpoch(epoch);

            // We have to get at least a majority of servers in sync with
            // us. We do this by waiting for the NEWLEADER packet to get
            // acknowledged
            try {
                waitForNewLeaderAck(self.getId(), zk.getZxid());// 等待确认新的Leader
            } catch (InterruptedException e) {
                shutdown("Waiting for a quorum of followers, only synced with sids: [ "
                        + getSidSetString(newLeaderProposal.ackSet) + " ]");
                HashSet<Long> followerSet = new HashSet<Long>();
                for (M2mLearnerHandler f : learners)
                    followerSet.add(f.getSid());

                if (self.getQuorumVerifier().containsQuorum(followerSet)) {
                    LOG.warn("Enough followers present. "
                            + "Perhaps the initTicks need to be increased.");
                }
                Thread.sleep(self.tickTime);
                self.tick++;
                return;
            }

            startZkServer();// 等待足够多的server确认新的Leader以后启动ZookeeperServer服务

            String initialZxid = System
                    .getProperty("zookeeper.testingonly.initialZxid");
            if (initialZxid != null) {
                long zxid = Long.parseLong(initialZxid);
                zk.setZxid((zk.getZxid() & 0xffffffff00000000L) | zxid);
            }
            /**
             * 设置对应的zooKeeperServer,方便CnxnFactory来的时候进行处理
             */
            if (!System.getProperty("zookeeper.leaderServes", "yes").equals(
                    "no")) {
                self.cnxnFactory.addZooKeeperServer(self.getHandleIp(), zk);
            }
            boolean tickSkip = true;

            while (true) {
                Thread.sleep(self.tickTime / 2);
                if (!tickSkip) {
                    self.tick++;
                }
                HashSet<Long> syncedSet = new HashSet<Long>();

                // lock on the followers when we use it.
                syncedSet.add(self.getId());

                for (M2mLearnerHandler f : getLearners()) {
                    // Synced set is used to check we have a supporting quorum,
                    // so only
                    // PARTICIPANT, not OBSERVER, learners should be used
                    if (f.synced()) {
                        syncedSet.add(f.getSid());
                    }
                    f.ping();
                }

                if (!tickSkip
                        && !self.getQuorumVerifier().containsQuorum(syncedSet)) {
                    // if (!tickSkip && syncedCount < self.quorumPeers.size() /
                    // 2) {
                    // Lost quorum, shutdown
                    shutdown("Not sufficient followers synced, only synced with sids: [ "
                            + getSidSetString(syncedSet) + " ]");
                    // make sure the order is the same!
                    // the leader goes to looking
                    return;
                }
                tickSkip = !tickSkip;
            }
        } finally {
        }
    }

    boolean isShutdown;

    /**
     * Close down all the LearnerHandlers
     */
    void shutdown(String reason) {
        LOG.info("Shutting down");

        if (isShutdown) {
            return;
        }

        LOG.info("Shutdown called", new Exception("shutdown Leader! reason: "
                + reason));

        if (cnxAcceptor != null) {
            cnxAcceptor.halt();
        }

        // NIO should not accept conenctions
        self.cnxnFactory.removeZookeeper(self.getHandleIp());
        try {
            ss.close();
        } catch (IOException e) {
            LOG.warn("Ignoring unexpected exception during close", e);
        }
        // clear all the connections
        self.cnxnFactory.closeAll();
        // shutdown the previous zk
        if (zk != null) {
            zk.shutdown();
        }
        synchronized (learners) {
            for (Iterator<M2mLearnerHandler> it = learners.iterator(); it
                    .hasNext();) {
                M2mLearnerHandler f = it.next();
                it.remove();
                f.shutdown();
            }
        }
        isShutdown = true;
        System.out.println("关闭完全");
    }

    /**
     * Keep a count of acks that are received by the leader for a particular
     * proposal
     * 
     * @param zxid
     *            the zxid of the proposal sent out
     * @param followerAddr
     */
    /**
     * 处理ack
     * 
     * @param sid
     * @param zxid
     * @param followerAddr
     */
    synchronized public void processAck(long sid, long zxid,
            SocketAddress followerAddr) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Ack zxid: 0x{}", Long.toHexString(zxid));
            for (Proposal p : outstandingProposals.values()) {
                long packetZxid = p.packet.getZxid();
                LOG.trace("outstanding proposal: 0x{}",
                        Long.toHexString(packetZxid));
            }
            LOG.trace("outstanding proposals all");
        }

        if ((zxid & 0xffffffffL) == 0) {
            /*
             * We no longer process NEWLEADER ack by this method. However, the
             * learner sends ack back to the leader after it gets UPTODATE so we
             * just ignore the message.
             */
            return;
        }

        if (outstandingProposals.size() == 0) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("outstanding is 0");
            }
            return;
        }
        if (lastCommitted >= zxid) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(
                        "proposal has already been committed, pzxid: 0x{} zxid: 0x{}",
                        Long.toHexString(lastCommitted), Long.toHexString(zxid));
            }
            // The proposal has already been committed
            return;
        }
        Proposal p = outstandingProposals.get(zxid);// 获取对应的Proposal
        if (p == null) {
            LOG.warn("Trying to commit future proposal: zxid 0x{} from {}",
                    Long.toHexString(zxid), followerAddr);
            return;
        }

        p.ackSet.add(sid);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Count for zxid: 0x{} is {}", Long.toHexString(zxid),
                    p.ackSet.size());
        }
        if (self.getQuorumVerifier().containsQuorum(p.ackSet)) {
            if (zxid != lastCommitted + 1) {
                LOG.warn("Commiting zxid 0x{} from {} not first!",
                        Long.toHexString(zxid), followerAddr);
                LOG.warn("First is 0x{}", Long.toHexString(lastCommitted + 1));
            }
            outstandingProposals.remove(zxid);
            if (p.m2mRequest != null) {
                toBeApplied.add(p);
            }

            if (p.m2mRequest == null) {
                LOG.warn("Going to commmit null request for proposal: {}", p);
            }
            commit(zxid);
            inform(p);
            zk.commitProcessor.commit(p.m2mRequest);
            if (pendingSyncs.containsKey(zxid)) {
                for (LearnerSyncRequest r : pendingSyncs.remove(zxid)) {
                    sendSync(r);
                }
            }
        }
    }

    static class ToBeAppliedRequestProcessor implements RequestProcessor {
        private RequestProcessor next;

        private ConcurrentLinkedQueue<Proposal> toBeApplied;

        /**
         * This request processor simply maintains the toBeApplied list. For
         * this to work next must be a FinalRequestProcessor and
         * FinalRequestProcessor.processRequest MUST process the request
         * synchronously!
         * 
         * @param next
         *            a reference to the FinalRequestProcessor
         */
        ToBeAppliedRequestProcessor(RequestProcessor next,
                ConcurrentLinkedQueue<Proposal> toBeApplied) {
            if (!(next instanceof M2mFinalRequestProcessor)) {
                throw new RuntimeException(
                        ToBeAppliedRequestProcessor.class.getName()
                                + " must be connected to "
                                + M2mFinalRequestProcessor.class.getName()
                                + " not " + next.getClass().getName());
            }
            this.toBeApplied = toBeApplied;
            this.next = next;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.apache.zookeeper.server.RequestProcessor#processRequest(org.apache
         * .zookeeper.server.Request)
         */
        public void processRequest(M2mRequest request)
                throws RequestProcessorException {
            next.processRequest(request);
            Proposal p = toBeApplied.peek();
            if (p != null && p.m2mRequest != null
                    && p.m2mRequest.zxid == request.zxid) {
                toBeApplied.remove();
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.apache.zookeeper.server.RequestProcessor#shutdown()
         */
        public void shutdown() {
            LOG.info("Shutting down");
            next.shutdown();
        }
    }

    /**
     * send a packet to all the followers ready to follow
     * 
     * @param qp
     *            the packet to be sent
     */
    void sendPacket(M2mQuorumPacket qp) {
        synchronized (forwardingFollowers) {
            for (M2mLearnerHandler f : forwardingFollowers) {
                f.queuePacket(qp);
            }
        }
    }

    long lastCommitted = -1;

    /**
     * Create a commit packet and send it to all the members of the quorum
     * 
     * @param zxid
     */
    /**
     * 创建一个commit packet,发送给Follower
     * 
     * @param zxid
     */
    public void commit(long zxid) {
        synchronized (this) {
            lastCommitted = zxid;
        }
        M2mQuorumPacket qp = new M2mQuorumPacket(M2mLeader.COMMIT, zxid, null);
        sendPacket(qp);
    }

    /**
     * Create an inform packet and send it to all observers.
     * 
     * @param zxid
     * @param proposal
     */
    public void inform(Proposal proposal) {
    }

    /**
     * 上一次的投票的zxid
     */
    long lastProposed;

    /**
     * Returns the current epoch of the leader.
     * 
     * @return
     */
    public long getEpoch() {
        return ZxidUtils.getEpochFromZxid(lastProposed);
    }

    @SuppressWarnings("serial")
    public static class XidRolloverException extends Exception {
        public XidRolloverException(String message) {
            super(message);
        }
    }

    /**
     * create a proposal and send it out to all the members
     * 
     * @param request
     * @return the proposal that is queued to send to all the members
     */
    /*
     * 发起一个投票
     */
    public Proposal propose(M2mRequest request) throws XidRolloverException {
        /**
         * Address the rollover issue. All lower 32bits set indicate a new
         * leader election. Force a re-election instead. See ZOOKEEPER-1277
         */
        if ((request.zxid & 0xffffffffL) == 0xffffffffL) {
            String msg = "zxid lower 32 bits have rolled over, forcing re-election, and therefore new epoch start";
            shutdown(msg);
            throw new XidRolloverException(msg);
        }
        System.out.println("开始处理投票数据包" + request.type + "::" + request.zxid
                + request);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        M2mBinaryOutputArchive boa = M2mBinaryOutputArchive.getArchive(baos);
        try {
            request.m2mTxnHeader.serialize(boa, "hdr");
            if (request.txn != null) {
                request.txn.serialize(boa, "txn");
            }
            baos.close();
        } catch (IOException e) {
            LOG.warn("This really should be impossible", e);
        }
        M2mQuorumPacket pp = new M2mQuorumPacket(M2mLeader.PROPOSAL,
                request.zxid, baos.toByteArray());

        Proposal p = new Proposal();
        p.packet = pp;
        p.m2mRequest = request;
        synchronized (this) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Proposing:: " + request);
            }

            lastProposed = p.packet.getZxid();
            outstandingProposals.put(lastProposed, p);// 添加到投票箱，方便今后确认
            sendPacket(pp);
        }
        return p;
    }

    /**
     * Process sync requests
     * 
     * @param r
     *            the request
     */
    /**
     * 处理所有的sync请求
     * 
     * @param r
     */
    synchronized public void processSync(LearnerSyncRequest r) {
        if (outstandingProposals.isEmpty()) {
            sendSync(r);
        } else {
            List<LearnerSyncRequest> l = pendingSyncs.get(lastProposed);
            if (l == null) {
                l = new ArrayList<LearnerSyncRequest>();
            }
            l.add(r);
            pendingSyncs.put(lastProposed, l);
        }
    }

    /**
     * Sends a sync message to the appropriate server
     * 
     * @param f
     * @param r
     */

    public void sendSync(LearnerSyncRequest r) {
        M2mQuorumPacket qp = new M2mQuorumPacket(M2mLeader.SYNC, 0, null);
        r.fh.queuePacket(qp);
    }

    /**
     * lets the leader know that a follower is capable of following and is done
     * syncing
     * 
     * @param handler
     *            handler of the follower
     * @return last proposed zxid
     */
    /**
     * 沒有应用的，但是已经完成ACK 确认的，让follower进行同步
     * 
     * @param handler
     * @param lastSeenZxid
     * @return
     */
    synchronized public long startForwarding(M2mLearnerHandler handler,
            long lastSeenZxid) {
        // Queue up any outstanding requests enabling the receipt of
        // new requests
        if (lastProposed > lastSeenZxid) {
            for (Proposal p : toBeApplied) {
                if (p.packet.getZxid() <= lastSeenZxid) {
                    continue;
                }
                handler.queuePacket(p.packet);
                // Since the proposal has been committed we need to send the
                // commit message also
                M2mQuorumPacket qp = new M2mQuorumPacket(M2mLeader.COMMIT,
                        p.packet.getZxid(), null);
                handler.queuePacket(qp);
            }

            List<Long> zxids = new ArrayList<Long>(
                    outstandingProposals.keySet());
            Collections.sort(zxids);
            for (Long zxid : zxids) {
                if (zxid <= lastSeenZxid) {
                    continue;
                }
                handler.queuePacket(outstandingProposals.get(zxid).packet);
            }

        }

        addForwardingFollower(handler);

        return lastProposed;
    }

    private HashSet<Long> connectingFollowers = new HashSet<Long>();

    /**
     * 等待Follower对新的sid以及epoch确认
     * 
     * @param sid
     * @param lastAcceptedEpoch
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    public long getEpochToPropose(long sid, long lastAcceptedEpoch)
            throws InterruptedException, IOException {
        synchronized (connectingFollowers) {
            if (!waitingForNewEpoch) {
                return epoch;
            }
            if (lastAcceptedEpoch >= epoch) {
                epoch = lastAcceptedEpoch + 1;
            }
            connectingFollowers.add(sid);
            M2mQuorumVerifier verifier = self.getQuorumVerifier();
            if (connectingFollowers.contains(self.getId())
                    && verifier.containsQuorum(connectingFollowers)) {
                waitingForNewEpoch = false;
                self.setAcceptedEpoch(epoch);
                connectingFollowers.notifyAll();
            } else {
                long start = System.currentTimeMillis();
                long cur = start;
                long end = start + self.getInitLimit() * self.getTickTime();
                while (waitingForNewEpoch && cur < end) {
                    connectingFollowers.wait(end - cur);
                    cur = System.currentTimeMillis();
                }
                if (waitingForNewEpoch) {
                    throw new InterruptedException(
                            "Timeout while waiting for epoch from quorum");
                }
            }
            return epoch;
        }
    }

    private HashSet<Long> electingFollowers = new HashSet<Long>();
    private boolean electionFinished = false;

    /**
     * 等待ACK的确认
     * 
     * @param id
     * @param ss
     * @throws IOException
     * @throws InterruptedException
     */
    public void waitForEpochAck(long id, StateSummary ss) throws IOException,
            InterruptedException {
        synchronized (electingFollowers) {
            if (electionFinished) {
                return;
            }
            if (ss.getCurrentEpoch() != -1) {
                if (ss.isMoreRecentThan(leaderStateSummary)) {
                    throw new IOException(
                            "Follower is ahead of the leader, leader summary: "
                                    + leaderStateSummary.getCurrentEpoch()
                                    + " (current epoch), "
                                    + leaderStateSummary.getLastZxid()
                                    + " (last zxid)");
                }
                electingFollowers.add(id);
            }
            M2mQuorumVerifier verifier = self.getQuorumVerifier();
            if (electingFollowers.contains(self.getId())
                    && verifier.containsQuorum(electingFollowers)) {
                electionFinished = true;
                electingFollowers.notifyAll();
            } else {
                long start = System.currentTimeMillis();
                long cur = start;
                long end = start + self.getInitLimit() * self.getTickTime();
                while (!electionFinished && cur < end) {
                    electingFollowers.wait(end - cur);
                    cur = System.currentTimeMillis();
                }
                if (!electionFinished) {
                    throw new InterruptedException(
                            "Timeout while waiting for epoch to be acked by quorum");
                }
            }
        }
    }

    /**
     * Return a list of sid in set as string
     */
    private String getSidSetString(Set<Long> sidSet) {
        StringBuilder sids = new StringBuilder();
        Iterator<Long> iter = sidSet.iterator();
        while (iter.hasNext()) {
            sids.append(iter.next());
            if (!iter.hasNext()) {
                break;
            }
            sids.append(",");
        }
        return sids.toString();
    }

    /**
     * Start up Leader ZooKeeper server and initialize zxid to the new epoch
     */
    private synchronized void startZkServer() {
        // Update lastCommitted and Db's zxid to a value representing the new
        // epoch
        lastCommitted = zk.getZxid();
        LOG.info("Have quorum of supporters, sids: [ "
                + getSidSetString(newLeaderProposal.ackSet)
                + " ]; starting up and setting last processed zxid: 0x{}",
                Long.toHexString(zk.getZxid()));
        zk.startup();
        /*
         * Update the election vote here to ensure that all members of the
         * ensemble report the same vote to new servers that start up and send
         * leader election notifications to the ensemble.
         * 
         * @see https://issues.apache.org/jira/browse/ZOOKEEPER-1732
         */
        self.updateElectionVote(getEpoch());

        zk.getZKDatabase().setlastProcessedZxid(zk.getZxid());
    }

    /**
     * Process NEWLEADER ack of a given sid and wait until the leader receives
     * sufficient acks.
     *
     * @param sid
     * @param learnerType
     * @throws InterruptedException
     */
    public void waitForNewLeaderAck(long sid, long zxid)
            throws InterruptedException {

        synchronized (newLeaderProposal.ackSet) {

            if (quorumFormed) {
                return;
            }

            long currentZxid = newLeaderProposal.packet.getZxid();
            if (zxid != currentZxid) {
                LOG.error("NEWLEADER ACK from sid: " + sid
                        + " is from a different epoch - current 0x"
                        + Long.toHexString(currentZxid) + " receieved 0x"
                        + Long.toHexString(zxid));
                return;
            }

            newLeaderProposal.ackSet.add(sid);

            if (self.getQuorumVerifier().containsQuorum(
                    newLeaderProposal.ackSet)) {
                quorumFormed = true;
                newLeaderProposal.ackSet.notifyAll();
            } else {
                long start = System.currentTimeMillis();
                long cur = start;
                long end = start + self.getInitLimit() * self.getTickTime();
                while (!quorumFormed && cur < end) {
                    newLeaderProposal.ackSet.wait(end - cur);
                    cur = System.currentTimeMillis();
                }
                if (!quorumFormed) {
                    throw new InterruptedException(
                            "Timeout while waiting for NEWLEADER to be acked by quorum");
                }
            }
        }
    }

    /**
     * Get string representation of a given packet type
     * 
     * @param packetType
     * @return string representing the packet type
     */
    public static String getPacketType(int packetType) {
        switch (packetType) {
        case DIFF:
            return "DIFF";
        case TRUNC:
            return "TRUNC";
        case SNAP:
            return "SNAP";
        case OBSERVERINFO:
            return "OBSERVERINFO";
        case NEWLEADER:
            return "NEWLEADER";
        case FOLLOWERINFO:
            return "FOLLOWERINFO";
        case UPTODATE:
            return "UPTODATE";
        case LEADERINFO:
            return "LEADERINFO";
        case ACKEPOCH:
            return "ACKEPOCH";
        case REQUEST:
            return "REQUEST";
        case PROPOSAL:
            return "PROPOSAL";
        case ACK:
            return "ACK";
        case COMMIT:
            return "COMMIT";
        case PING:
            return "PING";
        case REVALIDATE:
            return "REVALIDATE";
        case SYNC:
            return "SYNC";
        case INFORM:
            return "INFORM";
        default:
            return "UNKNOWN";
        }
    }
}
