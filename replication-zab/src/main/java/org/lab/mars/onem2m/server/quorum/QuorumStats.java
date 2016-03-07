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


public class QuorumStats {
    private final Provider provider;

    protected QuorumStats(Provider provider) {
        this.provider = provider;
    }

    public String getServerState() {
        return provider.getServerState();
    }

    public String[] getQuorumPeers() {
        return provider.getQuorumPeers();
    }

    public interface Provider {
        static final String UNKNOWN_STATE = "unknown";
        static final String LOOKING_STATE = "leaderelection";
        static final String LEADING_STATE = "leading";
        static final String FOLLOWING_STATE = "following";
        static final String OBSERVING_STATE = "observing";

        String[] getQuorumPeers();

        String getServerState();
    }


}
