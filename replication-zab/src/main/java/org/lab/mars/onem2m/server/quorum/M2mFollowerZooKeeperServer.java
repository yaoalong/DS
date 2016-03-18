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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import lab.mars.ds.ds.persistence.FileTxnLog;

import org.lab.mars.onem2m.jute.M2mRecord;
import org.lab.mars.onem2m.server.DSDatabase;
import org.lab.mars.onem2m.server.M2mFinalRequestProcessor;
import org.lab.mars.onem2m.server.M2mRequest;
import org.lab.mars.onem2m.server.M2mSyncRequestProcessor;
import org.lab.mars.onem2m.server.RequestProcessor;
import org.lab.mars.onem2m.txn.M2mTxnHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Just like the standard ZooKeeperServer. We just replace the request
 * processors: FollowerRequestProcessor -> CommitProcessor ->
 * FinalRequestProcessor
 * 
 * A SyncRequestProcessor is also spawned off to log proposals from the leader.
 */
public class M2mFollowerZooKeeperServer extends M2mLearnerZooKeeperServer {
    private static final Logger LOG = LoggerFactory
            .getLogger(M2mFollowerZooKeeperServer.class);

    M2mCommitProcessor commitProcessor;

    M2mSyncRequestProcessor syncProcessor;

    /*
     * Pending sync requests
     */
    ConcurrentLinkedQueue<M2mRequest> pendingSyncs;

    /**
     *
     * @param fileTxnLog
     * @param self
     * @param zkDb
     * @throws IOException
     */
    M2mFollowerZooKeeperServer(FileTxnLog fileTxnLog, M2mQuorumPeer self,
            DSDatabase zkDb) throws IOException {
        super(fileTxnLog, self.tickTime, self.minSessionTimeout,
                self.maxSessionTimeout, zkDb, self);
        this.pendingSyncs = new ConcurrentLinkedQueue<M2mRequest>();
    }

    public M2mFollower getFollower() {
        return self.follower;
    }

    @Override
    protected void setupRequestProcessors() {
        RequestProcessor finalProcessor = new M2mFinalRequestProcessor(this);
        commitProcessor = new M2mCommitProcessor(finalProcessor,
                Long.toString(getServerId()), true);
        commitProcessor.start();
        firstProcessor = new M2mFollowerRequestProcessor(this, commitProcessor);
        ((M2mFollowerRequestProcessor) firstProcessor).start();
        syncProcessor = new M2mSyncRequestProcessor(this,
                new M2mSendAckRequestProcessor((M2mLearner) getFollower()));
        syncProcessor.start();
    }

    LinkedBlockingQueue<M2mRequest> pendingTxns = new LinkedBlockingQueue<M2mRequest>();// 等待处理的事务

    public void logRequest(M2mTxnHeader hdr, M2mRecord txn) {
        M2mRequest request = new M2mRequest(null, 0, hdr.getType(), null);
        request.m2mTxnHeader = hdr;
        request.txn = txn;
        request.zxid = hdr.getZxid();
        if ((request.zxid & 0xffffffffL) != 0) {
            pendingTxns.add(request);
        }
        syncProcessor.processRequest(request);
    }

    /**
     * When a COMMIT message is received, eventually this method is called,
     * which matches up the zxid from the COMMIT with (hopefully) the head of
     * the pendingTxns queue and hands it to the commitProcessor to commit.
     * 
     * @param zxid
     *            - must correspond to the head of pendingTxns if it exists
     */
    public void commit(long zxid) {
        if (pendingTxns.size() == 0) {
            LOG.warn("Committing " + Long.toHexString(zxid)
                    + " without seeing txn");
            return;
        }
        long firstElementZxid = pendingTxns.element().zxid;
        if (firstElementZxid != zxid) {
            LOG.error("Committing zxid 0x" + Long.toHexString(zxid)
                    + " but next pending txn 0x"
                    + Long.toHexString(firstElementZxid));
            System.exit(12);
        }
        M2mRequest request = pendingTxns.remove();
        commitProcessor.commit(request);
    }

    synchronized public void sync() {
        if (pendingSyncs.size() == 0) {
            LOG.warn("Not expecting a sync.");
            return;
        }

        M2mRequest r = pendingSyncs.remove();
        commitProcessor.commit(r);
    }

    @Override
    public int getGlobalOutstandingLimit() {
        return super.getGlobalOutstandingLimit() / (self.getQuorumSize() - 1);
    }

    @Override
    public void shutdown() {
        LOG.info("Shutting down");
        try {
            super.shutdown();
        } catch (Exception e) {
            LOG.warn("Ignoring unexpected exception during shutdown", e);
        }
        try {
            if (syncProcessor != null) {
                syncProcessor.shutdown();
            }
        } catch (Exception e) {
            LOG.warn("Ignoring unexpected exception in syncprocessor shutdown",
                    e);
        }
    }

    @Override
    public String getState() {
        return "follower";
    }

    @Override
    public M2mLearner getLearner() {
        return getFollower();
    }
}
