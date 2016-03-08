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

import org.lab.mars.onem2m.server.DSDatabase;
import org.lab.mars.onem2m.server.ZooKeeperServer;

import java.io.PrintWriter;

/**
 * Abstract base class for all ZooKeeperServers that participate in a quorum.
 */
public abstract class QuorumZooKeeperServer extends ZooKeeperServer {
    protected final M2mQuorumPeer self;

    protected QuorumZooKeeperServer(int tickTime,
                                    int minSessionTimeout, int maxSessionTimeout,
                                    DSDatabase zkDb, M2mQuorumPeer self) {
        super(tickTime, minSessionTimeout, maxSessionTimeout,
                 zkDb);
        this.self = self;
    }

    @Override
    public void dumpConf(PrintWriter pwriter) {
        super.dumpConf(pwriter);

        pwriter.print("initLimit=");
        pwriter.println(self.getInitLimit());
        pwriter.print("syncLimit=");
        pwriter.println(self.getSyncLimit());
        pwriter.print("electionAlg=");
        pwriter.println(self.getElectionType());
        pwriter.print("electionPort=");
        pwriter.println(self.quorumPeers.get(self.getId()).electionAddr
                .getPort());
        pwriter.print("quorumPort=");
        pwriter.println(self.quorumPeers.get(self.getId()).addr.getPort());
        pwriter.print("peerType=");
    }
}
