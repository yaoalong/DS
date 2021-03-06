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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;

import org.lab.mars.onem2m.M2mKeeperException;
import org.lab.mars.onem2m.ZooDefs.OpCode;
import org.lab.mars.onem2m.jute.M2mBinaryInputArchive;
import org.lab.mars.onem2m.jute.M2mBinaryOutputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;
import org.lab.mars.onem2m.server.M2mByteBufferInputStream;
import org.lab.mars.onem2m.server.M2mRequest;
import org.lab.mars.onem2m.server.ZooTrace;
import org.lab.mars.onem2m.server.quorum.M2mLeader.Proposal;
import org.lab.mars.onem2m.server.util.ZxidUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * There will be an instance of this class created by the Leader for each
 * learner. All communication with a learner is handled by this class.
 */
public class M2mLearnerHandler extends Thread {
    private static final Logger LOG = LoggerFactory
            .getLogger(M2mLearnerHandler.class);

    protected final Socket sock;
    final M2mLeader leader;
    /**
     * 用来发送 并且是线程安全的
     */
    final LinkedBlockingQueue<M2mQuorumPacket> queuedPackets = new LinkedBlockingQueue<M2mQuorumPacket>();
    /**
     * If this packet is queued, the sender thread will exit
     */
    final M2mQuorumPacket proposalOfDeath = new M2mQuorumPacket();
    /**
     * ZooKeeper server identifier of this learner
     */
    protected long sid = 0;
    protected int version = 0x1;
    /**
     * Deadline for receiving the next ack. If we are bootstrapping then it's
     * based on the initLimit, if we are done bootstrapping it's based on the
     * syncLimit. Once the deadline is past this learner should be considered no
     * longer "sync'd" with the leader.
     */
    volatile long tickOfNextAckDeadline;
    private SyncLimitCheck syncLimitCheck = new SyncLimitCheck();

    /**
     * The packets to be sent to the learner
     */
    private M2mBinaryInputArchive ia;
    private M2mBinaryOutputArchive oa;

    ;
    private BufferedOutputStream bufferedOutput;

    M2mLearnerHandler(Socket sock, M2mLeader leader) throws IOException {
        super("LearnerHandler-" + sock.getRemoteSocketAddress());
        this.sock = sock;
        this.leader = leader;
        leader.addLearnerHandler(this);
    }

    static public String packetToString(M2mQuorumPacket p) {
        return null;
    }

    public Socket getSocket() {
        return sock;
    }

    long getSid() {
        return sid;
    }

    int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LearnerHandler ").append(sock);
        sb.append(" tickOfNextAckDeadline:").append(tickOfNextAckDeadline());
        sb.append(" synced?:").append(synced());
        sb.append(" queuedPacketLength:").append(queuedPackets.size());
        return sb.toString();
    }

    /**
     * This method will use the thread to send packets added to the
     * queuedPackets list
     *
     * @throws InterruptedException
     */
    /*
     * 不断去发送消息,对应的是一个follower
     */
    private void sendPackets() throws InterruptedException {
        while (true) {
            try {
                M2mQuorumPacket p;
                p = queuedPackets.poll();
                if (p == null) {
                    bufferedOutput.flush();
                    p = queuedPackets.take();// 如果为空则进行阻塞
                }
                if (p == proposalOfDeath) {
                    break;
                }

                oa.writeRecord(p, "packet");
            } catch (IOException e) {
                if (!sock.isClosed()) {
                    LOG.warn("Unexpected exception at " + this, e);
                    try {
                        // this will cause everything to shutdown on
                        // this learner handler and will help notify
                        // the learner/observer instantaneously
                        sock.close();
                    } catch (IOException ie) {
                        LOG.warn("Error closing socket for handler " + this, ie);
                    }
                }
                break;
            }
        }
    }

    /**
     * This thread will receive packets from the peer and process them and also
     * listen to new connections from new peers.
     */
    @Override
    public void run() {
        try {
            tickOfNextAckDeadline = leader.self.tick + leader.self.initLimit
                    + leader.self.syncLimit;

            ia = M2mBinaryInputArchive.getArchive(new BufferedInputStream(sock
                    .getInputStream()));
            bufferedOutput = new BufferedOutputStream(sock.getOutputStream());
            oa = M2mBinaryOutputArchive.getArchive(bufferedOutput);

            M2mQuorumPacket qp = new M2mQuorumPacket();
            ia.readRecord(qp, "packet");
            if (qp.getType() != M2mLeader.FOLLOWERINFO
                    && qp.getType() != M2mLeader.OBSERVERINFO) {
                LOG.error("First packet " + qp.toString()
                        + " is not FOLLOWERINFO or OBSERVERINFO!");
                return;
            }
            byte learnerInfoData[] = qp.getData();
            if (learnerInfoData != null) {
                if (learnerInfoData.length == 8) {
                    ByteBuffer bbsid = ByteBuffer.wrap(learnerInfoData);
                    this.sid = bbsid.getLong();
                } else {
                    M2mLearnerInfo li = new M2mLearnerInfo();
                    M2mByteBufferInputStream.byteBuffer2Record(
                            ByteBuffer.wrap(learnerInfoData), li);
                    this.sid = li.getServerid();
                    this.version = li.getProtocolVersion();
                }
            } else {
                this.sid = leader.followerCounter.getAndDecrement();
            }

            LOG.info("Follower sid: " + sid + " : info : "
                    + leader.self.quorumPeers.get(sid));

            long lastAcceptedEpoch = ZxidUtils.getEpochFromZxid(qp.getZxid());

            long peerLastZxid;
            StateSummary ss = null;
            long zxid = qp.getZxid();
            long newEpoch = leader.getEpochToPropose(this.getSid(),
                    lastAcceptedEpoch);// 在这里进行阻塞掉，获取newEpoch

            if (this.getVersion() < 0x10000) {
                // we are going to have to extrapolate the epoch information
                long epoch = ZxidUtils.getEpochFromZxid(zxid);
                ss = new StateSummary(epoch, zxid);
                // fake the message
                leader.waitForEpochAck(this.getSid(), ss);
            } else {
                byte ver[] = new byte[4];
                ByteBuffer.wrap(ver).putInt(0x10000);
                M2mQuorumPacket newEpochPacket = new M2mQuorumPacket(
                        M2mLeader.LEADERINFO, ZxidUtils.makeZxid(newEpoch, 0),
                        ver);
                oa.writeRecord(newEpochPacket, "packet");
                bufferedOutput.flush();// 发送newEpoch给follower
                M2mQuorumPacket ackEpochPacket = new M2mQuorumPacket();
                ia.readRecord(ackEpochPacket, "packet");
                if (ackEpochPacket.getType() != M2mLeader.ACKEPOCH) {
                    LOG.error(ackEpochPacket.toString() + " is not ACKEPOCH");
                    return;
                }
                ByteBuffer bbepoch = ByteBuffer.wrap(ackEpochPacket.getData());
                ss = new StateSummary(bbepoch.getInt(),
                        ackEpochPacket.getZxid());
                leader.waitForEpochAck(this.getSid(), ss);// 阻塞在epoch确认这里,下面开始同步
            }
            peerLastZxid = ss.getLastZxid();

            /* the default to send to the follower */
            int packetToSend = M2mLeader.SNAP;
            long zxidToSend = 0;
            long leaderLastZxid = 0;
            /** the packets that the follower needs to get updates from **/
            long updates = peerLastZxid;

            /*
             * we are sending the diff check if we have proposals in memory to
             * be able to send a diff to the
             */
            ReentrantReadWriteLock lock = leader.zk.getDSDatabase()
                    .getLogLock();// 主线程还在执行
            ReadLock rl = lock.readLock();
            try {
                rl.lock();
                final long maxCommittedLog = leader.zk.getDSDatabase()
                        .getMaxCommittedLog();
                final long minCommittedLog = leader.zk.getDSDatabase()
                        .getMinCommittedLog();
                LOG.info("Synchronizing with Follower sid: " + sid
                        + " maxCommittedLog=0x"
                        + Long.toHexString(maxCommittedLog)
                        + " minCommittedLog=0x"
                        + Long.toHexString(minCommittedLog)
                        + " peerLastZxid=0x" + Long.toHexString(peerLastZxid)
                        + "dfff" + peerLastZxid);
                // 看看是否还有需要处理的投票
                LinkedList<Proposal> proposals = leader.zk.getDSDatabase()
                        .getCommittedLog();
                if (proposals.size() != 0) {
                    LOG.debug("proposal size is {}", proposals.size());
                    if ((maxCommittedLog >= peerLastZxid)
                            && (minCommittedLog <= peerLastZxid)) {
                        LOG.debug("Sending proposals to follower");

                        // as we look through proposals, this variable keeps
                        // track of previous
                        // proposal Id.
                        long prevProposalZxid = minCommittedLog;

                        // Keep track of whether we are about to send the first
                        // packet.
                        // Before sending the first packet, we have to tell the
                        // learner
                        // whether to expect a trunc or a diff
                        boolean firstPacket = true;

                        // If we are here, we can use committedLog to sync with
                        // follower. Then we only need to decide whether to
                        // send trunc or not
                        packetToSend = M2mLeader.DIFF;
                        zxidToSend = maxCommittedLog;

                        for (Proposal propose : proposals) {
                            // skip the proposals the peer already has
                            if (propose.packet.getZxid() <= peerLastZxid) {
                                prevProposalZxid = propose.packet.getZxid();
                                continue;
                            } else {
                                // If we are sending the first packet, figure
                                // out whether to trunc
                                // in case the follower has some proposals that
                                // the leader doesn't
                                if (firstPacket) {
                                    firstPacket = false;
                                    // Does the peer have some proposals that
                                    // the leader hasn't seen yet
                                    if (prevProposalZxid < peerLastZxid) {
                                        // send a trunc message before sending
                                        // the diff
                                        packetToSend = M2mLeader.TRUNC;
                                        zxidToSend = prevProposalZxid;// 确认前面的不需要进行处理
                                        updates = zxidToSend;
                                    }
                                }
                                /*
                                 * 将事务发送到队列中，然后立即在队列中加入一个commit
                                 */
                                queuePacket(propose.packet);
                                M2mQuorumPacket qcommit = new M2mQuorumPacket(
                                        M2mLeader.COMMIT,
                                        propose.packet.getZxid(), null);
                                queuePacket(qcommit);
                            }
                        }
                    } else if (peerLastZxid > maxCommittedLog) { // 如果follower比leader还提前，则让follower进行回滚
                        LOG.debug(
                                "Sending TRUNC to follower zxidToSend=0x{} updates=0x{}",
                                Long.toHexString(maxCommittedLog),
                                Long.toHexString(updates));

                        packetToSend = M2mLeader.TRUNC;
                        zxidToSend = maxCommittedLog;
                        updates = zxidToSend;
                    } else {
                        LOG.warn("Unhandled proposal scenario");
                    }
                } else
                    try {
                        if (peerLastZxid == leader.zk.getDSDatabase()
                                .getLastProcessedZxid()) {// 如果follower和Leader保持同步，那么只需要发送一个DIFF包
                            // The leader may recently take a snapshot, so the
                            // committedLog
                            // is empty. We don't need to send snapshot if the
                            // follow
                            // is already sync with in-memory db.
                            LOG.debug(
                                    "committedLog is empty but leader and follower "
                                            + "are in sync, zxid=0x{}",
                                    Long.toHexString(peerLastZxid));
                            packetToSend = M2mLeader.DIFF;
                            zxidToSend = peerLastZxid;
                        } else {
                            // just let the state transfer happen
                            LOG.debug("proposals is empty");
                        }
                    } catch (M2mKeeperException e) {
                        e.printStackTrace();
                    }

                LOG.info("Sending " + M2mLeader.getPacketType(packetToSend));// 发送的类型
                leaderLastZxid = leader.startForwarding(this, updates);

            } finally {
                rl.unlock();
            }
            M2mQuorumPacket newLeaderQP = new M2mQuorumPacket(
                    M2mLeader.NEWLEADER, ZxidUtils.makeZxid(newEpoch, 0), null);
            if (getVersion() < 0x10000) {
                oa.writeRecord(newLeaderQP, "packet");
            } else {
                queuedPackets.add(newLeaderQP);
            }
            bufferedOutput.flush();
            // Need to set the zxidToSend to the latest zxid
            if (packetToSend == M2mLeader.SNAP) {
                try {
                    zxidToSend = leader.zk.getDSDatabase()
                            .getLastProcessedZxid();
                } catch (M2mKeeperException e) {
                    e.printStackTrace();
                }// 获取对应的zxid
            }
            oa.writeRecord(new M2mQuorumPacket(packetToSend, zxidToSend, null),
                    "packet");
            bufferedOutput.flush();

            /* if we are not truncating or sending a diff just send a snapshot */
            if (packetToSend == M2mLeader.SNAP) {
                LOG.info("Sending snapshot last zxid of peer is 0x"
                        + Long.toHexString(peerLastZxid) + " "
                        + " zxid of leader is 0x"
                        + Long.toHexString(leaderLastZxid)
                        + "sent zxid of db as 0x"
                        + Long.toHexString(zxidToSend));
                // Dump data to peer
                leader.zk.getDSDatabase().serializeSnapshot(peerLastZxid, oa);
                oa.writeString("BenWasHere", "signature");
            }
            bufferedOutput.flush();

            // Start sending packets
            new Thread() {
                public void run() {
                    Thread.currentThread().setName(
                            "Sender-" + sock.getRemoteSocketAddress());
                    try {
                        sendPackets();
                    } catch (InterruptedException e) {
                        LOG.warn("Unexpected interruption", e);
                    }
                }
            }.start();

            /*
             * Have to wait for the first ACK, wait until the leader is ready,
             * and only then we can start processing messages.
             */
            qp = new M2mQuorumPacket();
            ia.readRecord(qp, "packet");
            if (qp.getType() != M2mLeader.ACK) {
                LOG.error("Next packet was supposed to be an ACK");
                return;
            }
            LOG.info("Received NEWLEADER-ACK message from " + getSid());
            leader.waitForNewLeaderAck(getSid(), qp.getZxid());// 确认新的Leaer

            syncLimitCheck.start();

            // now that the ack has been processed expect the syncLimit
            sock.setSoTimeout(leader.self.tickTime * leader.self.syncLimit);

            /*
             * Wait until leader starts up
             */
            synchronized (leader.zk) {
                while (!leader.zk.isRunning() && !this.isInterrupted()) {
                    leader.zk.wait(20);
                }
            }
            // Mutation packets will be queued during the serialize,
            // so we need to mark when the peer can actually start
            // using the data
            //
            queuedPackets
                    .add(new M2mQuorumPacket(M2mLeader.UPTODATE, -1, null));
            // 读取消息然后进行处理
            while (true) {
                qp = new M2mQuorumPacket();
                ia.readRecord(qp, "packet");

                long traceMask = ZooTrace.SERVER_PACKET_TRACE_MASK;
                if (qp.getType() == M2mLeader.PING) {
                    traceMask = ZooTrace.SERVER_PING_TRACE_MASK;
                }
                if (LOG.isTraceEnabled()) {
                    ZooTrace.logQuorumPacket(LOG, traceMask, 'i', qp);
                }
                tickOfNextAckDeadline = leader.self.tick
                        + leader.self.syncLimit;

                ByteBuffer bb;
                int cxid;
                int type;

                switch (qp.getType()) {
                case M2mLeader.ACK:
                    syncLimitCheck.updateAck(qp.getZxid());
                    leader.processAck(this.sid, qp.getZxid(),
                            sock.getLocalSocketAddress());
                    break;
                case M2mLeader.PING:
                    // Process the touches
                    ByteArrayInputStream bis = new ByteArrayInputStream(
                            qp.getData());
                    DataInputStream dis = new DataInputStream(bis);
                    while (dis.available() > 0) {
                        long sess = dis.readLong();
                        int to = dis.readInt();
                        leader.zk.touch(sess, to);
                    }
                    break;
                case M2mLeader.REVALIDATE:
                    bis = new ByteArrayInputStream(qp.getData());
                    dis = new DataInputStream(bis);
                    long id = dis.readLong();
                    int to = dis.readInt();
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(bos);
                    dos.writeLong(id);
                    boolean valid = leader.zk.touch(id, to);
                    if (LOG.isTraceEnabled()) {
                        ZooTrace.logTraceMessage(LOG,
                                ZooTrace.SESSION_TRACE_MASK, "Session 0x"
                                        + Long.toHexString(id) + " is valid: "
                                        + valid);
                    }
                    dos.writeBoolean(valid);
                    qp.setData(bos.toByteArray());
                    queuedPackets.add(qp);
                    break;
                case M2mLeader.REQUEST:
                    bb = ByteBuffer.wrap(qp.getData());
                    cxid = bb.getInt();
                    type = bb.getInt();
                    bb = bb.slice();
                    M2mRequest si = null;
                    if (type == OpCode.sync) {
                        // si = new LearnerSyncRequest(this, sessionId, cxid,
                        // type, bb);
                    } else {
                        si = new M2mRequest(null, cxid, type, bb);
                    }
                    leader.zk.submitRequest(si);
                    break;
                default:
                }
            }
        } catch (IOException e) {
            if (sock != null && !sock.isClosed()) {
                LOG.error("Unexpected exception causing shutdown while sock "
                        + "still open", e);
                // close the socket to make sure the
                // other side can see it being close
                try {
                    sock.close();
                } catch (IOException ie) {
                    // do nothing
                }
            }
        } catch (InterruptedException e) {
            LOG.error("Unexpected exception causing shutdown", e);
        } finally {
            LOG.warn("******* GOODBYE "
                    + (sock != null ? sock.getRemoteSocketAddress() : "<null>")
                    + " ********");
            shutdown();
        }
    }

    /*
     * 将QuorumPacket转化为Request
     */
    protected void pRequest2Txn(int type, long zxid, M2mRequest nM2mRequest,
            M2mRecord record) {


    }

    public void shutdown() {
        // Send the packet of death
        try {
            queuedPackets.put(proposalOfDeath);
        } catch (InterruptedException e) {
            LOG.warn("Ignoring unexpected exception", e);
        }
        try {
            if (sock != null && !sock.isClosed()) {
                sock.close();
            }
        } catch (IOException e) {
            LOG.warn("Ignoring unexpected exception during socket close", e);
        }
        this.interrupt();
        leader.removeLearnerHandler(this);
    }

    public long tickOfNextAckDeadline() {
        return tickOfNextAckDeadline;
    }

    /**
     * ping calls from the leader to the peers
     */
    public void ping() {
        long id;
        if (syncLimitCheck.check(System.nanoTime())) {
            synchronized (leader) {
                id = leader.lastProposed;
            }
            M2mQuorumPacket ping = new M2mQuorumPacket(M2mLeader.PING, id, null);
            queuePacket(ping);
        } else {
            LOG.warn("Closing connection to peer due to transaction timeout.");
            shutdown();
        }
    }

    /*
     * 每一个handler对应一个列表
     */
    void queuePacket(M2mQuorumPacket p) {
        queuedPackets.add(p);
    }

    /**
     * 判断是否可以进行同步
     *
     * @return
     */
    public boolean synced() {
        return isAlive() && leader.self.tick <= tickOfNextAckDeadline;
    }

    /**
     * This class controls the time that the Leader has been waiting for
     * acknowledgement of a proposal from this Learner. If the time is above
     * syncLimit, the connection will be closed. It keeps track of only one
     * proposal at a time, when the ACK for that proposal arrives, it switches
     * to the last proposal received or clears the value if there is no pending
     * proposal.
     */
    private class SyncLimitCheck {
        private long currentZxid = 0;
        private long currentTime = 0;
        private long nextZxid = 0;
        private long nextTime = 0;

        public synchronized void start() {
        }

        public synchronized void updateAck(long zxid) {
            if (currentZxid == zxid) {
                currentTime = nextTime;
                currentZxid = nextZxid;
                nextTime = 0;
                nextZxid = 0;
            } else if (nextZxid == zxid) {
                LOG.warn("ACK for " + zxid + " received before ACK for "
                        + currentZxid + "!!!!");
                nextTime = 0;
                nextZxid = 0;
            }
        }

        public synchronized boolean check(long time) {
            if (currentTime == 0) {
                return true;
            } else {
                long msDelay = (time - currentTime) / 1000000;
                return (msDelay < (leader.self.tickTime * leader.self.syncLimit));
            }
        }
    }
}
