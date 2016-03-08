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

import org.lab.mars.onem2m.server.M2mRequest;
import org.lab.mars.onem2m.server.RequestProcessor;
import org.lab.mars.onem2m.server.M2mSyncRequestProcessor;
import org.lab.mars.onem2m.server.quorum.M2mLeader.XidRolloverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This RequestProcessor simply forwards requests to an AckRequestProcessor and
 * SyncRequestProcessor.
 */
public class M2mProposalRequestProcessor implements RequestProcessor {
    private static final Logger LOG = LoggerFactory
            .getLogger(M2mProposalRequestProcessor.class);

    M2mLeaderZooKeeperServer zks;

    RequestProcessor nextProcessor;

    M2mSyncRequestProcessor syncProcessor;

    public M2mProposalRequestProcessor(M2mLeaderZooKeeperServer zks,
                                       RequestProcessor nextProcessor) {
        this.zks = zks;
        this.nextProcessor = nextProcessor;
        M2mAckRequestProcessor ackProcessor = new M2mAckRequestProcessor(
                zks.getLeader());
        syncProcessor = new M2mSyncRequestProcessor(zks, ackProcessor);
    }

    /**
     * initialize this processor
     */
    public void initialize() {
        syncProcessor.start();
    }

    public void processRequest(M2mRequest request)
            throws RequestProcessorException {

        /*
         * In the following IF-THEN-ELSE block, we process syncs on the leader.
         * If the sync is coming from a follower, then the follower handler adds
         * it to syncHandler. Otherwise, if it is a client of the leader that
         * issued the sync command, then syncHandler won't contain the handler.
         * In this case, we add it to syncHandler, and call processRequest on
         * the next processor.
         */

        nextProcessor.processRequest(request);
        if (request.m2mTxnHeader != null) {
            // We need to sync and get consensus on any transactions
            try {
                zks.getLeader().propose(request);// 发起一个投票
            } catch (XidRolloverException e) {
                throw new RequestProcessorException(e.getMessage(), e);
            }
            syncProcessor.processRequest(request);

        }
    }

    public void shutdown() {
        LOG.info("Shutting down");
        nextProcessor.shutdown();
        syncProcessor.shutdown();
    }

}