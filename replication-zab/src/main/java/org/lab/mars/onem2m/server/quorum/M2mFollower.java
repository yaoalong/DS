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
import java.net.InetSocketAddress;

import org.lab.mars.onem2m.M2mKeeperException;
import org.lab.mars.onem2m.jute.M2mRecord;
import org.lab.mars.onem2m.server.util.M2mSerializeUtils;
import org.lab.mars.onem2m.server.util.ZxidUtils;
import org.lab.mars.onem2m.txn.M2mTxnHeader;

/**
 * This class has the control logic for the Follower.
 */
public class M2mFollower extends M2mLearner {

    private long lastQueued;// 上一次进入队列中的zxid
    // This is the same object as this.zk, but we cache the downcast op
    final M2mFollowerZooKeeperServer fzk;

    M2mFollower(M2mQuorumPeer self, M2mFollowerZooKeeperServer zk) {
        this.self = self;
        this.zk = zk;
        this.fzk = zk;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Follower ").append(sock);
        sb.append(" lastQueuedZxid:").append(lastQueued);
        sb.append(" pendingRevalidationCount:").append(
                pendingRevalidations.size());
        return sb.toString();
    }

    /**
     * the main method called by the follower to follow the leader
     *
     * @throws InterruptedException
     */
    void followLeader() throws InterruptedException {
        self.end_fle = System.currentTimeMillis();
        LOG.info("FOLLOWING - LEADER ELECTION TOOK - "
                + (self.end_fle - self.start_fle));
        self.start_fle = 0;
        self.end_fle = 0;
        try {
            InetSocketAddress addr = findLeader();
            try {
                connectToLeader(addr);
                long newEpochZxid = 0;
                try {
                    newEpochZxid = registerWithLeader(M2mLeader.FOLLOWERINFO);
                } catch (M2mKeeperException e) {
                    e.printStackTrace();
                }

                // check to see if the leader zxid is lower than ours
                // this should never happen but is just a safety check
                long newEpoch = ZxidUtils.getEpochFromZxid(newEpochZxid);
                if (newEpoch < self.getAcceptedEpoch()) {
                    LOG.error("Proposed leader epoch "
                            + ZxidUtils.zxidToString(newEpochZxid)
                            + " is less than our accepted epoch "
                            + ZxidUtils.zxidToString(self.getAcceptedEpoch()));
                    throw new IOException("Error: Epoch of leader is lower");
                }
                syncWithLeader(newEpochZxid);
                M2mQuorumPacket qp = new M2mQuorumPacket();
                while (self.isRunning()) {
                    readPacket(qp);
                    processPacket(qp);
                }
            } catch (IOException e) {
                LOG.warn("Exception when following the leader", e);
                try {
                    sock.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                // clear pending revalidations
                pendingRevalidations.clear();
            }
        } finally {
            zk.unregisterJMX((M2mLearner) this);
        }
    }

    /**
     * Examine the packet received in qp and dispatch based on its contents.
     * 
     * @param qp
     * @throws IOException
     */
    protected void processPacket(M2mQuorumPacket qp) throws IOException {
        switch (qp.getType()) {
        case M2mLeader.PING:
            ping(qp);
            break;
        case M2mLeader.PROPOSAL:
            M2mTxnHeader hdr = new M2mTxnHeader();
            M2mRecord txn = M2mSerializeUtils.deserializeTxn(qp.getData(), hdr);
            if (hdr.getZxid() != lastQueued + 1) {
                LOG.warn("Got zxid 0x" + Long.toHexString(hdr.getZxid())
                        + " expected 0x" + Long.toHexString(lastQueued + 1));
            }
            lastQueued = hdr.getZxid();
            fzk.logRequest(hdr, txn);
            break;
        case M2mLeader.COMMIT:
            fzk.commit(qp.getZxid());
            break;
        case M2mLeader.UPTODATE:
            LOG.error("Received an UPTODATE message after Follower started");
            break;
        case M2mLeader.REVALIDATE:
            revalidate(qp);
            break;
        case M2mLeader.SYNC:
            fzk.sync();
            break;
        }
    }

    /**
     * The zxid of the last operation seen
     * 
     * @return zxid
     */
    public long getZxid() {
        try {
            synchronized (fzk) {
                return fzk.getZxid();
            }
        } catch (NullPointerException e) {
            LOG.warn("error getting zxid", e);
        }
        return -1;
    }

    /**
     * The zxid of the last operation queued
     * 
     * @return zxid
     */
    protected long getLastQueued() {
        return lastQueued;
    }

    @Override
    public void shutdown() {
        LOG.info("shutdown called", new Exception("shutdown Follower"));
        super.shutdown();
    }
}
