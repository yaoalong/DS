package org.lab.mars.onem2m.server.quorum;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.lab.mars.onem2m.M2mKeeperException;
import org.lab.mars.onem2m.jute.M2mBinaryInputArchive;
import org.lab.mars.onem2m.jute.M2mBinaryOutputArchive;
import org.lab.mars.onem2m.jute.M2mInputArchive;
import org.lab.mars.onem2m.jute.M2mOutputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;
import org.lab.mars.onem2m.server.M2mRequest;
import org.lab.mars.onem2m.server.ServerCnxn;
import org.lab.mars.onem2m.server.ZooTrace;
import org.lab.mars.onem2m.server.quorum.M2mQuorumPeer.QuorumServer;
import org.lab.mars.onem2m.server.util.M2mSerializeUtils;
import org.lab.mars.onem2m.server.util.ZxidUtils;
import org.lab.mars.onem2m.txn.M2mTxnHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is the superclass of two of the three main actors in a ZK
 * ensemble: Followers and Observers. Both Followers and Observers share a good
 * deal of code which is moved into Peer to avoid duplication.
 */
public class M2mLearner {
    static class PacketInFlight {
        M2mTxnHeader hdr;
        M2mRecord rec;
    }

    M2mQuorumPeer self;
    M2mLearnerZooKeeperServer zk;

    protected BufferedOutputStream bufferedOutput;

    protected Socket sock;

    /**
     * Socket getter
     * 
     * @return
     */
    public Socket getSocket() {
        return sock;
    }

    protected M2mInputArchive leaderIs;
    protected M2mOutputArchive leaderOs;
    /** the protocol version of the leader */
    protected int leaderProtocolVersion = 0x01;

    protected static final Logger LOG = LoggerFactory
            .getLogger(M2mLearner.class);

    static final private boolean nodelay = System.getProperty(
            "follower.nodelay", "true").equals("true");
    static {
        LOG.info("TCP NoDelay set to: " + nodelay);
    }

    final ConcurrentHashMap<Long, ServerCnxn> pendingRevalidations = new ConcurrentHashMap<Long, ServerCnxn>();

    public int getPendingRevalidationsCount() {
        return pendingRevalidations.size();
    }

    /**
     * validate a session for a client
     *
     * @param clientId
     *            the client to be revalidated
     * @param timeout
     *            the timeout for which the session is valid
     * @return
     * @throws IOException
     */
    void validateSession(ServerCnxn cnxn, long clientId, int timeout)
            throws IOException {
        LOG.info("Revalidating client: 0x" + Long.toHexString(clientId));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeLong(clientId);
        dos.writeInt(timeout);
        dos.close();
        M2mQuorumPacket qp = new M2mQuorumPacket(M2mLeader.REVALIDATE, -1,
                baos.toByteArray());
        pendingRevalidations.put(clientId, cnxn);
        if (LOG.isTraceEnabled()) {
            ZooTrace.logTraceMessage(LOG, ZooTrace.SESSION_TRACE_MASK,
                    "To validate session 0x" + Long.toHexString(clientId));
        }
        writePacket(qp, true);
    }

    /**
     * write a packet to the leader
     *
     * @param pp
     *            the proposal packet to be sent to the leader
     * @throws IOException
     */
    /**
     * 发送一个新的数据包
     * 
     * @param pp
     * @param flush
     * @throws IOException
     */
    void writePacket(M2mQuorumPacket pp, boolean flush) throws IOException {
        synchronized (leaderOs) {
            if (pp != null) {
                leaderOs.writeRecord(pp, "packet");
            }
            if (flush) {
                bufferedOutput.flush();
            }
        }
    }

    /**
     * read a packet from the leader
     *
     * @param pp
     *            the packet to be instantiated
     * @throws IOException
     */
    void readPacket(M2mQuorumPacket pp) throws IOException {
        synchronized (leaderIs) {
            leaderIs.readRecord(pp, "packet");
        }
        long traceMask = ZooTrace.SERVER_PACKET_TRACE_MASK;
        if (pp.getType() == M2mLeader.PING) {
            traceMask = ZooTrace.SERVER_PING_TRACE_MASK;
        }
        if (LOG.isTraceEnabled()) {
            ZooTrace.logQuorumPacket(LOG, traceMask, 'i', pp);
        }
    }

    /**
     * send a request packet to the leader
     *
     * @param request
     *            the request from the client
     * @throws IOException
     */
    /*
     * follower向leader发送请求
     */
    void request(M2mRequest request) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream oa = new DataOutputStream(baos);
        oa.writeInt(request.cxid);
        oa.writeInt(request.type);
        if (request.request != null) {
            request.request.rewind();
            int len = request.request.remaining();
            byte b[] = new byte[len];
            request.request.get(b);
            request.request.rewind();
            oa.write(b);
        }
        oa.close();
        // 封装成为一个最基本的QuorumPacket
        M2mQuorumPacket qp = new M2mQuorumPacket(M2mLeader.REQUEST, -1,
                baos.toByteArray());
        writePacket(qp, true);
    }

    /**
     * Returns the address of the node we think is the leader.
     */
    /**
     * 根据选票我们可以获取Leader节点
     * 
     * @return
     */
    protected InetSocketAddress findLeader() {
        InetSocketAddress addr = null;
        // Find the leader by id
        M2mVote current = self.getCurrentVote();
        for (QuorumServer s : self.getView().values()) {
            if (s.id == current.getId()) {
                addr = s.addr;
                break;
            }
        }
        if (addr == null) {
            LOG.warn("Couldn't find the leader with id = " + current.getId());
        }
        return addr;
    }

    /**
     * Establish a connection with the Leader found by findLeader. Retries 5
     * times before giving up.
     * 
     * @param addr
     *            - the address of the Leader to connect to.
     * @throws IOException
     *             - if the socket connection fails on the 5th attempt
     * @throws ConnectException
     * @throws InterruptedException
     */
    protected void connectToLeader(InetSocketAddress addr) throws IOException,
            ConnectException, InterruptedException {
        sock = new Socket();
        sock.setSoTimeout(self.tickTime * self.initLimit);
        for (int tries = 0; tries < 5; tries++) {
            try {
                sock.connect(addr, self.tickTime * self.syncLimit);
                sock.setTcpNoDelay(nodelay);
                break;
            } catch (IOException e) {
                if (tries == 4) {
                    LOG.error("Unexpected exception", e);
                    throw e;
                } else {
                    LOG.warn("Unexpected exception, tries=" + tries
                            + ", connecting to " + addr, e);
                    sock = new Socket();
                    sock.setSoTimeout(self.tickTime * self.initLimit);
                }
            }
            Thread.sleep(1000);
        }
        leaderIs = M2mBinaryInputArchive.getArchive(new BufferedInputStream(
                sock.getInputStream()));
        bufferedOutput = new BufferedOutputStream(sock.getOutputStream());
        leaderOs = M2mBinaryOutputArchive.getArchive(bufferedOutput);
    }

    /**
     * Once connected to the leader, perform the handshake protocol to establish
     * a following / observing connection.
     * 
     * @param pktType
     * @return the zxid the Leader sends for synchronization purposes.
     * @throws IOException
     * @throws M2mKeeperException
     */
    protected long registerWithLeader(int pktType) throws IOException,
            M2mKeeperException {
        /*
         * Send follower info, including last zxid and sid
         */
        long lastLoggedZxid = self.getLastLoggedZxid();
        M2mQuorumPacket qp = new M2mQuorumPacket();
        qp.setType(pktType);
        qp.setZxid(ZxidUtils.makeZxid(self.getAcceptedEpoch(), 0));

        /*
         * Add sid to payload
         */
        M2mLearnerInfo li = new M2mLearnerInfo(self.getId(), 0x10000);
        ByteArrayOutputStream bsid = new ByteArrayOutputStream();
        M2mBinaryOutputArchive boa = M2mBinaryOutputArchive.getArchive(bsid);
        boa.writeRecord(li, "LearnerInfo");
        qp.setData(bsid.toByteArray());

        writePacket(qp, true);
        readPacket(qp);
        final long newEpoch = ZxidUtils.getEpochFromZxid(qp.getZxid());
        if (qp.getType() == M2mLeader.LEADERINFO) {
            // we are connected to a 1.0 server so accept the new epoch and read
            // the next packet
            leaderProtocolVersion = ByteBuffer.wrap(qp.getData()).getInt();
            byte epochBytes[] = new byte[4];
            final ByteBuffer wrappedEpochBytes = ByteBuffer.wrap(epochBytes);
            if (newEpoch > self.getAcceptedEpoch()) {
                wrappedEpochBytes.putInt((int) self.getCurrentEpoch());
                self.setAcceptedEpoch(newEpoch);
            } else if (newEpoch == self.getAcceptedEpoch()) {
                // since we have already acked an epoch equal to the leaders, we
                // cannot ack
                // again, but we still need to send our lastZxid to the leader
                // so that we can
                // sync with it if it does assume leadership of the epoch.
                // the -1 indicates that this reply should not count as an ack
                // for the new epoch
                wrappedEpochBytes.putInt(-1);
            } else {
                throw new IOException("Leaders epoch, " + newEpoch
                        + " is less than accepted epoch, "
                        + self.getAcceptedEpoch());
            }
            M2mQuorumPacket ackNewEpoch = new M2mQuorumPacket(
                    M2mLeader.ACKEPOCH, lastLoggedZxid, epochBytes);// 发送ack
            writePacket(ackNewEpoch, true);
            return ZxidUtils.makeZxid(newEpoch, 0);
        } else {
            if (newEpoch > self.getAcceptedEpoch()) {
                self.setAcceptedEpoch(newEpoch);
            }
            if (qp.getType() != M2mLeader.NEWLEADER) {
                LOG.error("First packet should have been NEWLEADER");
                throw new IOException("First packet should have been NEWLEADER");
            }
            return qp.getZxid();
        }
    }

    /**
     * Finally, synchronize our history with the Leader.
     * 
     * @param newLeaderZxid
     * @throws IOException
     * @throws InterruptedException
     */
    protected void syncWithLeader(long newLeaderZxid) throws IOException,
            InterruptedException {
        M2mQuorumPacket ack = new M2mQuorumPacket(M2mLeader.ACK, 0, null);
        M2mQuorumPacket qp = new M2mQuorumPacket();
        long newEpoch = ZxidUtils.getEpochFromZxid(newLeaderZxid);

        readPacket(qp);
        LinkedList<Long> packetsCommitted = new LinkedList<Long>();
        LinkedList<PacketInFlight> packetsNotCommitted = new LinkedList<PacketInFlight>();
        synchronized (zk) {
            if (qp.getType() == M2mLeader.DIFF) {
                LOG.info("Getting a diff from the leader 0x"
                        + Long.toHexString(qp.getZxid()));
            } else if (qp.getType() == M2mLeader.SNAP) {
                LOG.info("Getting a snapshot from leader");
                // The leader is going to dump the database
                // clear our own database and read
                zk.getDSDatabase().clear();
                zk.getDSDatabase().deserializeSnapshot(leaderIs);
                String signature = leaderIs.readString("signature");
                if (!signature.equals("BenWasHere")) {
                    LOG.error("Missing signature. Got " + signature);
                    throw new IOException("Missing signature");
                }
                zk.getDSDatabase().commit();
                zk.getDSDatabase().clear();
            } else if (qp.getType() == M2mLeader.TRUNC) {
                // we need to truncate the log to the lastzxid of the leader
                LOG.warn("Truncating log to get in sync with the leader 0x"
                        + Long.toHexString(qp.getZxid()));
                boolean truncated = false;
                try {
                    truncated = zk.getDSDatabase().truncateLog(qp.getZxid());
                } catch (M2mKeeperException e) {
                    e.printStackTrace();
                }
                if (!truncated) {
                    // not able to truncate the log
                    LOG.error("Not able to truncate the log "
                            + Long.toHexString(qp.getZxid()));
                    System.exit(13);
                }

            } else {
                LOG.error("Got unexpected packet from leader " + qp.getType()
                        + " exiting ... ");
                System.exit(13);

            }
            zk.getDSDatabase().setlastProcessedZxid(qp.getZxid());
            zk.createSessionTracker();

            long lastQueued = 0;

            // in V1.0 we take a snapshot when we get the NEWLEADER message, but
            // in pre V1.0
            // we take the snapshot at the UPDATE, since V1.0 also gets the
            // UPDATE (after the NEWLEADER)
            // we need to make sure that we don't take the snapshot twice.
            boolean snapshotTaken = false;
            // we are now going to start getting transactions to apply followed
            // by an UPTODATE
            outerLoop: while (self.isRunning()) {
                readPacket(qp);
                switch (qp.getType()) {
                case M2mLeader.PROPOSAL:
                    PacketInFlight pif = new PacketInFlight();
                    pif.hdr = new M2mTxnHeader();
                    pif.rec = M2mSerializeUtils.deserializeTxn(qp.getData(),
                            pif.hdr);
                    if (pif.hdr.getZxid() != lastQueued + 1) {
                        LOG.warn("Got zxid 0x"
                                + Long.toHexString(pif.hdr.getZxid())
                                + " expected 0x"
                                + Long.toHexString(lastQueued + 1));
                    }
                    lastQueued = pif.hdr.getZxid();
                    packetsNotCommitted.add(pif);
                    break;
                case M2mLeader.COMMIT:
                    if (!snapshotTaken) {
                        pif = packetsNotCommitted.peekFirst();
                        if (pif.hdr.getZxid() != qp.getZxid()) {
                            LOG.warn("Committing " + qp.getZxid()
                                    + ", but next proposal is "
                                    + pif.hdr.getZxid());
                        } else {
                            try {
                                zk.processTxn(pif.hdr, pif.rec);
                            } catch (M2mKeeperException e) {
                                e.printStackTrace();
                            }
                            packetsNotCommitted.remove();
                        }
                    } else {
                        packetsCommitted.add(qp.getZxid());
                    }
                    break;
                case M2mLeader.INFORM:
                    /*
                     * Only observer get this type of packet. We treat this as
                     * receiving PROPOSAL and COMMMIT.
                     */
                    PacketInFlight packet = new PacketInFlight();
                    packet.hdr = new M2mTxnHeader();
                    packet.rec = M2mSerializeUtils.deserializeTxn(qp.getData(),
                            packet.hdr);
                    // Log warning message if txn comes out-of-order
                    if (packet.hdr.getZxid() != lastQueued + 1) {
                        LOG.warn("Got zxid 0x"
                                + Long.toHexString(packet.hdr.getZxid())
                                + " expected 0x"
                                + Long.toHexString(lastQueued + 1));
                    }
                    lastQueued = packet.hdr.getZxid();
                    if (!snapshotTaken) {
                        // Apply to db directly if we haven't taken the snapshot
                        try {
                            zk.processTxn(packet.hdr, packet.rec);
                        } catch (M2mKeeperException e) {
                            e.printStackTrace();
                        }
                    } else {
                        packetsNotCommitted.add(packet);
                        packetsCommitted.add(qp.getZxid());
                    }
                    break;
                case M2mLeader.UPTODATE:
                    if (!snapshotTaken) { // true for the pre v1.0 case
                        self.setCurrentEpoch(newEpoch);
                    }
                    self.cnxnFactory.addZooKeeperServer(self.getHandleIp(), zk);
                    break outerLoop;
                case M2mLeader.NEWLEADER: // it will be NEWLEADER in v1.0
                    // Create updatingEpoch file and remove it after current
                    // epoch is set. QuorumPeer.loadDataBase() uses this file to
                    // detect the case where the server was terminated after
                    // taking a snapshot but before setting the current epoch.
                    // File updating = new
                    // File(self.getTxnFactory().getSnapDir(),
                    // QuorumPeer.UPDATING_EPOCH_FILENAME);
                    // if (!updating.exists() && !updating.createNewFile()) {
                    // throw new IOException("Failed to create "
                    // + updating.toString());
                    // }
                    // zk.takeSnapshot();
                    // self.setCurrentEpoch(newEpoch);
                    // if (!updating.delete()) {
                    // throw new IOException("Failed to delete "
                    // + updating.toString());
                    // }
                    snapshotTaken = true;
                    writePacket(new M2mQuorumPacket(M2mLeader.ACK,
                            newLeaderZxid, null), true);
                    break;
                }
            }
        }
        ack.setZxid(ZxidUtils.makeZxid(newEpoch, 0));
        writePacket(ack, true);
        sock.setSoTimeout(self.tickTime * self.syncLimit);
        zk.startup();
        /*
         * Update the election vote here to ensure that all members of the
         * ensemble report the same vote to new servers that start up and send
         * leader election notifications to the ensemble.
         * 
         * @see https://issues.apache.org/jira/browse/ZOOKEEPER-1732
         */
        self.updateElectionVote(newEpoch);

        // We need to log the stuff that came in between the snapshot and the
        // uptodate
        if (zk instanceof M2mFollowerZooKeeperServer) {
            M2mFollowerZooKeeperServer fzk = (M2mFollowerZooKeeperServer) zk;
            for (PacketInFlight p : packetsNotCommitted) {
                fzk.logRequest(p.hdr, p.rec);
            }
            for (Long zxid : packetsCommitted) {
                fzk.commit(zxid);
            }
        } else {
            // New server type need to handle in-flight packets
            throw new UnsupportedOperationException("Unknown server type");
        }
    }

    protected void revalidate(M2mQuorumPacket qp) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(qp.getData());
        DataInputStream dis = new DataInputStream(bis);
        long sessionId = dis.readLong();
        boolean valid = dis.readBoolean();
        ServerCnxn cnxn = pendingRevalidations.remove(sessionId);
        if (cnxn == null) {
            LOG.warn("Missing session 0x" + Long.toHexString(sessionId)
                    + " for validation");
        } else {
            zk.finishSessionInit(cnxn, valid);
        }
        if (LOG.isTraceEnabled()) {
            ZooTrace.logTraceMessage(LOG, ZooTrace.SESSION_TRACE_MASK,
                    "Session 0x" + Long.toHexString(sessionId) + " is valid: "
                            + valid);
        }
    }

    /**
     * 开始ping
     * 
     * @param qp
     * @throws IOException
     */
    protected void ping(M2mQuorumPacket qp) throws IOException {
        // Send back the ping with our session data
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        HashMap<Long, Integer> touchTable = zk.getTouchSnapshot();
        for (Entry<Long, Integer> entry : touchTable.entrySet()) {
            dos.writeLong(entry.getKey());
            dos.writeInt(entry.getValue());
        }
        qp.setData(bos.toByteArray());
        writePacket(qp, true);
    }

    /**
     * Shutdown the Peer
     */
    public void shutdown() {
        // set the zookeeper server to null
        self.cnxnFactory.removeZookeeper(self.getHandleIp());
        // clear all the connections
        self.cnxnFactory.closeAll();
        // shutdown previous zookeeper
        if (zk != null) {
            zk.shutdown();
        }
    }
}
