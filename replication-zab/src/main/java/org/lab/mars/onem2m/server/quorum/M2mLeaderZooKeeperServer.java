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

import org.lab.mars.ds.server.*;
import org.lab.mars.onem2m.server.M2mFinalRequestProcessor;
import org.lab.mars.onem2m.server.PrepRequestProcessor;
import org.lab.mars.onem2m.server.RequestProcessor;
import org.lab.mars.onem2m.server.ServerCnxn;
import org.lab.mars.onem2m.server.ZKDatabase;

import java.io.IOException;

/**
 * Just like the standard ZooKeeperServer. We just replace the request
 * processors: PrepRequestProcessor -> ProposalRequestProcessor ->
 * CommitProcessor -> Leader.ToBeAppliedRequestProcessor ->
 * FinalRequestProcessor
 */
public class M2mLeaderZooKeeperServer extends QuorumZooKeeperServer {
    M2mCommitProcessor commitProcessor;


    M2mLeaderZooKeeperServer(M2mQuorumPeer self,
                             DataTreeBuilder treeBuilder, ZKDatabase zkDb) throws IOException {
        super(self.tickTime, self.minSessionTimeout,
                self.maxSessionTimeout, treeBuilder, zkDb, self);
    }

    public M2mLeader getLeader() {
        return self.leader;
    }

    @Override
    protected void setupRequestProcessors() {
        RequestProcessor finalProcessor = new M2mFinalRequestProcessor(this);
        RequestProcessor toBeAppliedProcessor = new M2mLeader.ToBeAppliedRequestProcessor(
                finalProcessor, getLeader().toBeApplied);
        commitProcessor = new M2mCommitProcessor(toBeAppliedProcessor,
                Long.toString(getServerId()), false);
        commitProcessor.start();
        M2mProposalRequestProcessor proposalProcessor = new M2mProposalRequestProcessor(
                this, commitProcessor);
        proposalProcessor.initialize();
        firstProcessor = new PrepRequestProcessor(this, proposalProcessor);
        ((PrepRequestProcessor) firstProcessor).start();
    }

    @Override
    public int getGlobalOutstandingLimit() {
        return super.getGlobalOutstandingLimit() / (self.getQuorumSize() - 1);
    }

    @Override
    public void createSessionTracker() {

    }

    @Override
    protected void startSessionTracker() {
        // ((SessionTrackerImpl) sessionTracker).start();
    }

    public boolean touch(long sess, int to) {
        return sessionTracker.touchSession(sess, to);
    }

    @Override
    protected void registerJMX() {

    }

    @Override
    protected void unregisterJMX() {
    }

    protected void unregisterJMX(M2mLeader leader) {
    }

    @Override
    public String getState() {
        return "leader";
    }

    /**
     * Returns the id of the associated QuorumPeer, which will do for a unique
     * id of this server.
     */
    @Override
    public long getServerId() {
        return self.getId();
    }

    @Override
    protected void revalidateSession(ServerCnxn cnxn, long sessionId,
                                     int sessionTimeout) throws IOException {

    }
}
