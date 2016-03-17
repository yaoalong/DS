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

import org.lab.mars.onem2m.ZooDefs.OpCode;
import org.lab.mars.onem2m.server.M2mRequest;
import org.lab.mars.onem2m.server.RequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * This RequestProcessor forwards any requests that modify the state of the
 * system to the Leader.
 */
public class M2mFollowerRequestProcessor extends Thread implements
        RequestProcessor {
    private static final Logger LOG = LoggerFactory
            .getLogger(M2mFollowerRequestProcessor.class);

    M2mFollowerZooKeeperServer zks;

    RequestProcessor nextProcessor;

    LinkedBlockingQueue<M2mRequest> queuedRequests = new LinkedBlockingQueue<M2mRequest>();

    boolean finished = false;

    public M2mFollowerRequestProcessor(M2mFollowerZooKeeperServer zks,
                                       RequestProcessor nextProcessor) {
        super("FollowerRequestProcessor:" + zks.getServerId());
        this.zks = zks;
        this.nextProcessor = nextProcessor;
    }

    @Override
    public void run() {
        try {
            while (!finished) {
                M2mRequest request = queuedRequests.take();
                if (request == M2mRequest.requestOfDeath) {
                    break;
                }

                nextProcessor.processRequest(request);
                switch (request.type) {
                    case OpCode.sync: // sync这个操作可以暂时忽略
                        zks.pendingSyncs.add(request);
                        zks.getFollower().request(request);
                        break;
                    case OpCode.create:
                    case OpCode.delete:
                    case OpCode.setData:
                        zks.getFollower().request(request);
                        break;
                }
            }
        } catch (Exception e) {
            LOG.error("Unexpected exception causing exit", e);
        }
        LOG.info("FollowerRequestProcessor exited loop!");
    }

    public void processRequest(M2mRequest request) {
        if (!finished) {
            queuedRequests.add(request);
        }
    }

    public void shutdown() {
        LOG.info("Shutting down");
        finished = true;
        queuedRequests.clear();
        queuedRequests.add(M2mRequest.requestOfDeath);
        nextProcessor.shutdown();
    }

}
