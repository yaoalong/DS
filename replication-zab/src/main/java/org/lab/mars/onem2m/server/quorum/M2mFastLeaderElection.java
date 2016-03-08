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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.lab.mars.onem2m.server.quorum.M2mQuorumCnxManager.Message;
import org.lab.mars.onem2m.server.quorum.M2mQuorumPeer.QuorumServer;
import org.lab.mars.onem2m.server.quorum.M2mQuorumPeer.ServerState;
import org.lab.mars.onem2m.server.util.ZxidUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of leader election using TCP. It uses an object of the class
 * QuorumCnxManager to manage connections. Otherwise, the algorithm is
 * push-based as with the other UDP implementations.
 *
 * There are a few parameters that can be tuned to change its behavior. First,
 * finalizeWait determines the amount of time to wait until deciding upon a
 * leader. This is part of the leader election algorithm.
 */

public class M2mFastLeaderElection implements M2mElection {
    private static final Logger LOG = LoggerFactory
            .getLogger(M2mFastLeaderElection.class);

    /**
     * Determine how much time a process has to wait once it believes that it
     * has reached the end of leader election.
     */
    final static int finalizeWait = 200;

    /**
     * Upper bound on the amount of time between two consecutive notification
     * checks. This impacts the amount of time to get the system up again after
     * long partitions. Currently 60 seconds.
     */

    final static int maxNotificationInterval = 60000;

    /**
     * Connection manager. Fast leader election uses TCP for communication
     * between peers, and QuorumCnxManager manages such connections.
     */

    M2mQuorumCnxManager manager;

    /**
     * Notifications are messages that let other peers know that a given peer
     * has changed its vote, either because it has joined leader election or
     * because it learned of another peer with higher zxid or same zxid and
     * higher server id
     */
    /*
     * 让其他peer
     */
    static public class Notification {
        /*
         * Format version, introduced in 3.4.6
         */

        public final static int CURRENTVERSION = 0x1;
        int version;

        /*
         * Proposed leader
         */
        long leader;

        /*
         * zxid of the proposed leader
         */
        long zxid;

        /*
         * Epoch
         */
        long electionEpoch;

        /*
         * current state of sender
         */
        ServerState state;

        /*
         * Address of sender
         */
        long sid;

        /*
         * epoch of the proposed leader
         */
        long peerEpoch;

        @Override
        public String toString() {
            return new String(Long.toHexString(version)
                    + " (message format version), " + leader
                    + " (n.leader), 0x" + Long.toHexString(zxid)
                    + " (n.zxid), 0x" + Long.toHexString(electionEpoch)
                    + " (n.round), " + state + " (n.state), " + sid
                    + " (n.sid), 0x" + Long.toHexString(peerEpoch)
                    + " (n.peerEpoch) ");
        }
    }

    /**
     * 构建消息
     * 
     * @param state
     * @param leader
     * @param zxid
     * @param electionEpoch
     * @param epoch
     * @return
     */
    static ByteBuffer buildMsg(int state, long leader, long zxid,
            long electionEpoch, long epoch) {
        byte requestBytes[] = new byte[40];
        ByteBuffer requestBuffer = ByteBuffer.wrap(requestBytes);

        /*
         * Building notification packet to send
         */

        requestBuffer.clear();
        requestBuffer.putInt(state);
        requestBuffer.putLong(leader);
        requestBuffer.putLong(zxid);
        requestBuffer.putLong(electionEpoch);
        requestBuffer.putLong(epoch);
        requestBuffer.putInt(Notification.CURRENTVERSION);

        return requestBuffer;
    }

    /**
     * Messages that a peer wants to send to other peers. These messages can be
     * both Notifications and Acks of reception of notification.
     */
    /**
     *
     * @author yaoalong
     * @Date 2016年1月25日
     * @Email yaoalong@foxmail.com
     */
    static public class ToSend {
        static enum mType {
            crequest, challenge, notification, ack
        }

        ToSend(mType type, long leader, long zxid, long electionEpoch,
                ServerState state, long sid, long peerEpoch) {

            this.leader = leader;
            this.zxid = zxid;
            this.electionEpoch = electionEpoch;
            this.state = state;
            this.sid = sid;
            this.peerEpoch = peerEpoch;
        }

        /*
         * Proposed leader in the case of notification
         */
        long leader;

        /*
         * id contains the tag for acks, and zxid for notifications
         */
        long zxid;

        /*
         * Epoch
         */
        long electionEpoch;

        /*
         * Current state;
         */
        ServerState state;

        /*
         * Address of recipient
         */
        long sid;

        /*
         * Leader epoch
         */
        long peerEpoch;
    }

    LinkedBlockingQueue<ToSend> sendqueue;
    LinkedBlockingQueue<Notification> recvqueue;

    /**
     * Multi-threaded implementation of message handler. Messenger implements
     * two sub-classes: WorkReceiver and WorkSender. The functionality of each
     * is obvious from the name. Each of these spawns a new thread.
     */
    /**
     * Message不断的运行，获取消息 如果接受到选举完成后再加进来的
     * 
     * @author yaoalong
     * @Date 2016年1月25日
     * @Email yaoalong@foxmail.com
     */
    protected class Messenger {

        /**
         * Receives messages from instance of QuorumCnxManager on method run(),
         * and processes such messages.
         */

        class WorkerReceiver implements Runnable {
            volatile boolean stop;
            M2mQuorumCnxManager manager;

            WorkerReceiver(M2mQuorumCnxManager manager) {
                this.stop = false;
                this.manager = manager;
            }

            public void run() {

                Message response;
                while (!stop) {
                    // Sleeps on receive
                    try {
                        response = manager.pollRecvQueue(3000,
                                TimeUnit.MILLISECONDS);
                        if (response == null)
                            continue;

                        /*
                         * If it is from an observer, respond right away. Note
                         * that the following predicate assumes that if a server
                         * is not a follower, then it must be an observer. If we
                         * ever have any other type of learner in the future,
                         * we'll have to change the way we check for observers.
                         */
                        if (!self.getVotingView().containsKey(response.sid)) {
                            M2mVote current = self.getCurrentVote();
                            ToSend notmsg = new ToSend(
                                    ToSend.mType.notification, current.getId(),
                                    current.getZxid(), logicalclock,
                                    self.getPeerState(), response.sid,
                                    current.getPeerEpoch());// 把自己当前的信息发送给它

                            sendqueue.offer(notmsg);
                        } else {
                            // Receive new message
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Receive new notification message. My id = "
                                        + self.getId());
                            }

                            /*
                             * We check for 28 bytes for backward compatibility
                             */
                            if (response.buffer.capacity() < 28) {
                                LOG.error("Got a short response: "
                                        + response.buffer.capacity());
                                continue;
                            }
                            boolean backCompatibility = (response.buffer
                                    .capacity() == 28);
                            response.buffer.clear();

                            // Instantiate Notification and set its attributes
                            Notification n = new Notification();

                            // State of peer that sent this message
                            ServerState ackstate = ServerState.LOOKING;

                            switch (response.buffer.getInt()) {
                            case 0:
                                ackstate = ServerState.LOOKING;
                                break;
                            case 1:
                                ackstate = ServerState.FOLLOWING;
                                break;
                            case 2:
                                ackstate = ServerState.LEADING;
                                break;
                            default:
                                continue;
                            }

                            n.leader = response.buffer.getLong();
                            n.zxid = response.buffer.getLong();
                            n.electionEpoch = response.buffer.getLong();
                            n.state = ackstate;
                            n.sid = response.sid;
                            if (!backCompatibility) {
                                n.peerEpoch = response.buffer.getLong();
                            } else {
                                if (LOG.isInfoEnabled()) {
                                    LOG.info("Backward compatibility mode, server id="
                                            + n.sid);
                                }
                                n.peerEpoch = ZxidUtils
                                        .getEpochFromZxid(n.zxid);
                            }

                            /*
                             * Version added in 3.4.6
                             */

                            n.version = (response.buffer.remaining() >= 4) ? response.buffer
                                    .getInt() : 0x0;

                            /*
                             * Print notification info
                             */
                            if (LOG.isInfoEnabled()) {
                                printNotification(n);
                            }

                            /*
                             * If this server is looking, then send proposed
                             * leader
                             */

                            if (self.getPeerState() == ServerState.LOOKING) {
                                printNotification(n);
                                recvqueue.offer(n);

                                /*
                                 * Send a notification back if the peer that
                                 * sent this message is also looking and its
                                 * logical clock is lagging behind.
                                 */
                                if ((ackstate == ServerState.LOOKING)
                                        && (n.electionEpoch < logicalclock)) {
                                    M2mVote v = getVote();
                                    ToSend notmsg = new ToSend(
                                            ToSend.mType.notification,
                                            v.getId(), v.getZxid(),
                                            logicalclock, self.getPeerState(),
                                            response.sid, v.getPeerEpoch());
                                    sendqueue.offer(notmsg);
                                }
                            } else {
                                /*
                                 * If this server is not looking, but the one
                                 * that sent the ack is looking, then send back
                                 * what it believes to be the leader.
                                 */
                                M2mVote current = self.getCurrentVote();
                                if (ackstate == ServerState.LOOKING) {
                                    if (LOG.isDebugEnabled()) {
                                        LOG.debug("Sending new notification. My id =  "
                                                + self.getId()
                                                + " recipient="
                                                + response.sid
                                                + " zxid=0x"
                                                + Long.toHexString(current
                                                        .getZxid())
                                                + " leader=" + current.getId());
                                    }

                                    ToSend notmsg;
                                    if (n.version > 0x0) {
                                        notmsg = new ToSend(
                                                ToSend.mType.notification,
                                                current.getId(),
                                                current.getZxid(),
                                                current.getElectionEpoch(),
                                                self.getPeerState(),
                                                response.sid,
                                                current.getPeerEpoch());

                                    } else {
                                        M2mVote bcVote = self.getBCVote();
                                        notmsg = new ToSend(
                                                ToSend.mType.notification,
                                                bcVote.getId(),
                                                bcVote.getZxid(),
                                                bcVote.getElectionEpoch(),
                                                self.getPeerState(),
                                                response.sid,
                                                bcVote.getPeerEpoch());
                                    }
                                    sendqueue.offer(notmsg);// 如果自己已经从选举过程中恢复，那么直接将自己的选举结果告诉给对方
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        System.out
                                .println("Interrupted Exception while waiting for new message"
                                        + e.toString());
                    }
                }
                LOG.info("WorkerReceiver is down");
            }
        }

        /**
         * This worker simply dequeues a message to send and and queues it on
         * the manager's queue.
         */

        class WorkerSender implements Runnable {
            volatile boolean stop;
            M2mQuorumCnxManager manager;

            WorkerSender(M2mQuorumCnxManager manager) {
                this.stop = false;
                this.manager = manager;
            }

            public void run() {
                while (!stop) {
                    try {
                        ToSend m = sendqueue.poll(3000, TimeUnit.MILLISECONDS);
                        if (m == null)
                            continue;

                        process(m);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                LOG.info("WorkerSender is down");
            }

            /**
             * Called by run() once there is a new message to send.
             *
             * @param m
             *            message to send
             */
            void process(ToSend m) {
                ByteBuffer requestBuffer = buildMsg(m.state.ordinal(),
                        m.leader, m.zxid, m.electionEpoch, m.peerEpoch);
                /**
                 * 调用manager进行发送
                 */
                manager.toSend(m.sid, requestBuffer);
            }
        }

        /**
         * Test if both send and receive queues are empty.
         */
        public boolean queueEmpty() {
            return (sendqueue.isEmpty() || recvqueue.isEmpty());
        }

        WorkerSender ws;
        WorkerReceiver wr;

        /**
         * Constructor of class Messenger.
         *
         * @param manager
         *            Connection manager
         */
        Messenger(M2mQuorumCnxManager manager) {

            this.ws = new WorkerSender(manager);

            Thread t = new Thread(this.ws, "WorkerSender[myid=" + self.getId()
                    + "]");
            t.setDaemon(true);
            t.start();

            this.wr = new WorkerReceiver(manager);

            t = new Thread(this.wr, "WorkerReceiver[myid=" + self.getId() + "]");
            t.setDaemon(true);
            t.start();
        }

        /**
         * Stops instances of WorkerSender and WorkerReceiver
         */
        void halt() {
            this.ws.stop = true;
            this.wr.stop = true;
        }

    }

    M2mQuorumPeer self;
    Messenger messenger;
    volatile long logicalclock; /* Election instance */
    long proposedLeader;
    long proposedZxid;
    long proposedEpoch;

    /**
     * Returns the current vlue of the logical clock counter
     */
    public long getLogicalClock() {
        return logicalclock;
    }

    /**
     * Constructor of FastLeaderElection. It takes two parameters, one is the
     * QuorumPeer object that instantiated this object, and the other is the
     * connection manager. Such an object should be created only once by each
     * peer during an instance of the ZooKeeper service.
     *
     * @param self
     *            QuorumPeer that created this object
     * @param manager
     *            Connection manager
     */
    public M2mFastLeaderElection(M2mQuorumPeer self, M2mQuorumCnxManager manager) {
        this.stop = false;
        this.manager = manager;
        starter(self, manager);
    }

    /**
     * This method is invoked by the constructor. Because it is a part of the
     * starting procedure of the object that must be on any constructor of this
     * class, it is probably best to keep as a separate method. As we have a
     * single constructor currently, it is not strictly necessary to have it
     * separate.
     *
     * @param self
     *            QuorumPeer that created this object
     * @param manager
     *            Connection manager
     */
    /**
     * 开始启动
     * 
     * @param self
     * @param manager
     */
    private void starter(M2mQuorumPeer self, M2mQuorumCnxManager manager) {
        this.self = self;
        proposedLeader = -1;
        proposedZxid = -1;

        sendqueue = new LinkedBlockingQueue<ToSend>();
        recvqueue = new LinkedBlockingQueue<Notification>();
        this.messenger = new Messenger(manager);
    }

    /**
     * 选举Leader完成
     */
    private void leaveInstance(M2mVote v) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("About to leave FLE instance: leader=" + v.getId()
                    + ", zxid=0x" + Long.toHexString(v.getZxid()) + ", my id="
                    + self.getId() + ", my state=" + self.getPeerState());
        }
        recvqueue.clear();
    }

    public M2mQuorumCnxManager getCnxManager() {
        return manager;
    }

    volatile boolean stop;

    public void shutdown() {
        stop = true;
        LOG.debug("Shutting down connection manager");
        manager.halt();
        LOG.debug("Shutting down messenger");
        messenger.halt();
        LOG.debug("FLE is down");
    }

    /**
     * Send notifications to all peers upon a change in our vote
     */
    /**
     * 发送通知消息
     */
    private void sendNotifications() {
        for (QuorumServer server : self.getVotingView().values()) {
            long sid = server.id;

            ToSend notmsg = new ToSend(ToSend.mType.notification,
                    proposedLeader, proposedZxid, logicalclock,
                    ServerState.LOOKING, sid, proposedEpoch);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Sending Notification: " + proposedLeader
                        + " (n.leader), 0x" + Long.toHexString(proposedZxid)
                        + " (n.zxid), 0x" + Long.toHexString(logicalclock)
                        + " (n.round), " + sid + " (recipient), "
                        + self.getId() + " (myid), 0x"
                        + Long.toHexString(proposedEpoch) + " (n.peerEpoch)");
            }
            sendqueue.offer(notmsg);
        }
    }

    private void printNotification(Notification n) {
        LOG.info("Notification: " + n.toString() + self.getPeerState()
                + " (my state)");
    }

    /**
     * Check if a pair (server id, zxid) succeeds our current vote.
     *
     * @param id
     *            Server identifier
     * @param zxid
     *            Last zxid observed by the issuer of this vote
     */
    /**
     * 首先判断周期、然后是zxid、然后是id
     * 
     * @param newId
     * @param newZxid
     * @param newEpoch
     * @param curId
     * @param curZxid
     * @param curEpoch
     * @return
     */
    protected boolean totalOrderPredicate(long newId, long newZxid,
            long newEpoch, long curId, long curZxid, long curEpoch) {
        LOG.debug("id: " + newId + ", proposed id: " + curId + ", zxid: 0x"
                + Long.toHexString(newZxid) + ", proposed zxid: 0x"
                + Long.toHexString(curZxid));

        /*
         * We return true if one of the following three cases hold: 1- New epoch
         * is higher 2- New epoch is the same as current epoch, but new zxid is
         * higher 3- New epoch is the same as current epoch, new zxid is the
         * same as current zxid, but server id is higher.
         */

        return ((newEpoch > curEpoch) || ((newEpoch == curEpoch) && ((newZxid > curZxid) || ((newZxid == curZxid) && (newId > curId)))));
    }

    /**
     *
     * @param votes
     * @param vote
     * @return
     */
    protected boolean termPredicate(HashMap<Long, M2mVote> votes, M2mVote vote) {

        HashSet<Long> set = new HashSet<Long>();

        /*
         * First make the views consistent. Sometimes peers will have different
         * zxids for a server depending on timing.
         */
        for (Map.Entry<Long, M2mVote> entry : votes.entrySet()) {
            if (vote.equals(entry.getValue())) {
                set.add(entry.getKey());
            }
        }

        return self.getQuorumVerifier().containsQuorum(set);
    }

    /**
     * In the case there is a leader elected, and a quorum supporting this
     * leader, we have to check if the leader has voted and acked that it is
     * leading. We need this check to avoid that peers keep electing over and
     * over a peer that has crashed and it is no longer leading.
     *
     * @param votes
     *            set of votes
     * @param leader
     *            leader id
     * @param electionEpoch
     *            epoch id
     */
    protected boolean checkLeader(HashMap<Long, M2mVote> votes, long leader,
                                  long electionEpoch) {

        boolean predicate = true;

        /*
         * If everyone else thinks I'm the leader, I must be the leader. The
         * other two checks are just for the case in which I'm not the leader.
         * If I'm not the leader and I haven't received a message from leader
         * stating that it is leading, then predicate is false.
         */

        if (leader != self.getId()) {
            if (votes.get(leader) == null)
                predicate = false;
            else if (votes.get(leader).getState() != ServerState.LEADING)
                predicate = false;
        } else if (logicalclock != electionEpoch) {
            predicate = false;
        }

        return predicate;
    }

    /**
     * This predicate checks that a leader has been elected. It doesn't make a
     * lot of sense without context (check lookForLeader) and it has been
     * separated for testing purposes.
     * 
     * @param recv
     *            map of received votes
     * @param ooe
     *            map containing out of election votes (LEADING or FOLLOWING)
     * @param n
     *            Notification
     * @return
     */
    protected boolean ooePredicate(HashMap<Long, M2mVote> recv,
                                   HashMap<Long, M2mVote> ooe, Notification n) {

        return (termPredicate(recv, new M2mVote(n.version, n.leader, n.zxid,
                n.electionEpoch, n.peerEpoch, n.state)) && checkLeader(ooe,
                n.leader, n.electionEpoch));

    }

    synchronized void updateProposal(long leader, long zxid, long epoch) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Updating proposal: " + leader + " (newleader), 0x"
                    + Long.toHexString(zxid) + " (newzxid), " + proposedLeader
                    + " (oldleader), 0x" + Long.toHexString(proposedZxid)
                    + " (oldzxid)");
        }
        proposedLeader = leader;
        proposedZxid = zxid;
        proposedEpoch = epoch;
    }

    /**
     * 我自己的投票
     * 
     * @return
     */
    synchronized M2mVote getVote() {
        return new M2mVote(proposedLeader, proposedZxid, proposedEpoch);
    }

    /**
     * A learning state can be either FOLLOWING or OBSERVING. This method simply
     * decides which one depending on the role of the server.
     *
     * @return ServerState
     */
    private ServerState learningState() {
        LOG.debug("I'm a participant: " + self.getId());
        return ServerState.FOLLOWING;

    }

    /**
     * Returns the initial vote value of server identifier.
     *
     * @return long
     */
    private long getInitId() {

        return self.getId();

    }

    /**
     * Returns initial last logged zxid.
     *
     * @return long
     */
    private long getInitLastLoggedZxid() {

        return self.getLastLoggedZxid();

    }

    /**
     * Returns the initial vote value of the peer epoch.
     *
     * @return long
     */
    private long getPeerEpoch() {

        try {
            return self.getCurrentEpoch();
        } catch (IOException e) {
            RuntimeException re = new RuntimeException(e.getMessage());
            re.setStackTrace(e.getStackTrace());
            throw re;
        }

    }

    /**
     * Starts a new round of leader election. Whenever our QuorumPeer changes
     * its state to LOOKING, this method is invoked, and it sends notifications
     * to all other peers.
     */
    /**
     * 寻找特定的Leader
     */
    public M2mVote lookForLeader() throws InterruptedException {

        if (self.start_fle == 0) {
            self.start_fle = System.currentTimeMillis();
        }
        try {
            HashMap<Long, M2mVote> recvset = new HashMap<Long, M2mVote>();

            HashMap<Long, M2mVote> outofelection = new HashMap<Long, M2mVote>();

            int notTimeout = finalizeWait;

            synchronized (this) {
                logicalclock++;
                updateProposal(getInitId(), getInitLastLoggedZxid(),
                        getPeerEpoch());
            }

            LOG.info("New election. My id =  " + self.getId()
                    + ", proposed zxid=0x" + Long.toHexString(proposedZxid));
            sendNotifications();

            /*
             * Loop in which we exchange notifications until we find a leader
             */

            while ((self.getPeerState() == ServerState.LOOKING) && (!stop)) {
                /*
                 * Remove next notification from queue, times out after 2 times
                 * the termination time
                 */
                Notification n = recvqueue.poll(notTimeout,
                        TimeUnit.MILLISECONDS);

                /*
                 * Sends more notifications if haven't received enough.
                 * Otherwise processes new notification.
                 */
                /**
                 * 如果接收队列为空，那么就可能是没有任何连接
                 */
                if (n == null) {
                    if (manager.haveDelivered()) {
                        sendNotifications();
                    } else {
                        manager.connectAll();
                    }

                    /*
                     * Exponential backoff
                     */
                    int tmpTimeOut = notTimeout * 2;
                    notTimeout = (tmpTimeOut < maxNotificationInterval ? tmpTimeOut
                            : maxNotificationInterval);
                    LOG.info("Notification time out: " + notTimeout);
                } else if (self.getVotingView().containsKey(n.sid)) {
                    /*
                     * Only proceed if the vote comes from a replica in the
                     * voting view.
                     */
                    switch (n.state) {
                    case LOOKING:
                        // If notification > current, replace and send messages
                        // out
                        /**
                         * 如果当前的选举周期太低，那么就清除已经接收到的投票，然后进行过官博转发
                         */
                        if (n.electionEpoch > logicalclock) {
                            logicalclock = n.electionEpoch;
                            recvset.clear();
                            if (totalOrderPredicate(n.leader, n.zxid,
                                    n.peerEpoch, getInitId(),
                                    getInitLastLoggedZxid(), getPeerEpoch())) {
                                updateProposal(n.leader, n.zxid, n.peerEpoch);
                            } else {
                                updateProposal(getInitId(),
                                        getInitLastLoggedZxid(), getPeerEpoch());
                            }
                            sendNotifications();
                        } else if (n.electionEpoch < logicalclock) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Notification election epoch is smaller than logicalclock. n.electionEpoch = 0x"
                                        + Long.toHexString(n.electionEpoch)
                                        + ", logicalclock=0x"
                                        + Long.toHexString(logicalclock));
                            }
                            break;
                        } else if (totalOrderPredicate(n.leader, n.zxid,
                                n.peerEpoch, proposedLeader, proposedZxid,
                                proposedEpoch)) {
                            updateProposal(n.leader, n.zxid, n.peerEpoch);
                            sendNotifications();
                        }

                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Adding vote: from=" + n.sid
                                    + ", proposed leader=" + n.leader
                                    + ", proposed zxid=0x"
                                    + Long.toHexString(n.zxid)
                                    + ", proposed election epoch=0x"
                                    + Long.toHexString(n.electionEpoch));
                        }

                        recvset.put(n.sid, new M2mVote(n.leader, n.zxid,
                                n.electionEpoch, n.peerEpoch)); // 某一个已经投了我现在的选择了

                        if (termPredicate(recvset, new M2mVote(proposedLeader,
                                proposedZxid, logicalclock, proposedEpoch))) {

                            // Verify if there is any change in the proposed
                            // leader
                            while ((n = recvqueue.poll(finalizeWait,
                                    TimeUnit.MILLISECONDS)) != null) {
                                if (totalOrderPredicate(n.leader, n.zxid,
                                        n.peerEpoch, proposedLeader,
                                        proposedZxid, proposedEpoch)) {
                                    recvqueue.put(n);
                                    break;
                                }
                            }

                            /*
                             * This predicate is true once we don't read any new
                             * relevant message from the reception queue
                             */
                            if (n == null) {
                                self.setPeerState((proposedLeader == self
                                        .getId()) ? ServerState.LEADING
                                        : learningState());

                                M2mVote endVote = new M2mVote(proposedLeader,
                                        proposedZxid, logicalclock,
                                        proposedEpoch);
                                leaveInstance(endVote);
                                return endVote;
                            }
                        }
                        break;
                    case FOLLOWING:
                    case LEADING:
                        /*
                         * Consider all notifications from the same epoch
                         * together.
                         */
                        if (n.electionEpoch == logicalclock) {
                            recvset.put(n.sid, new M2mVote(n.leader, n.zxid,
                                    n.electionEpoch, n.peerEpoch));

                            if (ooePredicate(recvset, outofelection, n)) {
                                self.setPeerState((n.leader == self.getId()) ? ServerState.LEADING
                                        : learningState());

                                M2mVote endVote = new M2mVote(n.leader, n.zxid,
                                        n.electionEpoch, n.peerEpoch);
                                leaveInstance(endVote);
                                return endVote;
                            }
                        }

                        /*
                         * Before joining an established ensemble, verify a
                         * majority is following the same leader.
                         */
                        outofelection.put(n.sid, new M2mVote(n.version, n.leader,
                                n.zxid, n.electionEpoch, n.peerEpoch, n.state));

                        if (ooePredicate(outofelection, outofelection, n)) {
                            synchronized (this) {
                                logicalclock = n.electionEpoch;
                                self.setPeerState((n.leader == self.getId()) ? ServerState.LEADING
                                        : learningState());
                            }
                            M2mVote endVote = new M2mVote(n.leader, n.zxid,
                                    n.electionEpoch, n.peerEpoch);
                            leaveInstance(endVote);
                            return endVote;
                        }
                        break;
                    default:
                        LOG.warn(
                                "Notification state unrecognized: {} (n.state), {} (n.sid)",
                                n.state, n.sid);
                        break;
                    }
                } else {
                    LOG.warn("Ignoring notification from non-cluster member "
                            + n.sid);
                }
            }
            return null;
        } finally {

        }
    }
}