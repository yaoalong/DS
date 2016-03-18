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

import lab.mars.ds.ds.persistence.FileTxnLog;
import org.lab.mars.onem2m.server.ServerCnxn;
import org.lab.mars.onem2m.server.DSDatabase;

import java.io.IOException;
import java.util.HashMap;

/**
 * Parent class for all ZooKeeperServers for Learners
 */
public abstract class M2mLearnerZooKeeperServer extends QuorumZooKeeperServer {
    public M2mLearnerZooKeeperServer(FileTxnLog fileTxnLog, int tickTime,
                                     int minSessionTimeout, int maxSessionTimeout,
                                     DSDatabase zkDb, M2mQuorumPeer self)
            throws IOException {
        super(fileTxnLog,tickTime, minSessionTimeout, maxSessionTimeout,
                 zkDb, self);
    }

    /**
     * Abstract method to return the learner associated with this server. Since
     * the Learner may change under our feet (when QuorumPeer reassigns it) we
     * can't simply take a reference here. Instead, we need the subclasses to
     * implement this.
     */
    abstract public M2mLearner getLearner();

    /**
     * Returns the current state of the session tracker. This is only currently
     * used by a Learner to build a ping response packet.
     *
     */
    protected HashMap<Long, Integer> getTouchSnapshot() {
        if (sessionTracker != null) {
            return ((LearnerSessionTracker) sessionTracker).snapshot();
        }
        return new HashMap<Long, Integer>();
    }


    @Override
    public long getServerId() {
        return self.getId();
    }

    @Override
    public void createSessionTracker() {
    }

    @Override
    protected void startSessionTracker() {
    }

    @Override
    protected void revalidateSession(ServerCnxn cnxn, long sessionId,
                                     int sessionTimeout) throws IOException {
        getLearner().validateSession(cnxn, sessionId, sessionTimeout);
    }

    @Override
    protected void registerJMX() {
    }

    @Override
    protected void unregisterJMX() {

    }

    protected void unregisterJMX(M2mLearner peer) {
    }
}
