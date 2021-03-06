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

import java.io.Flushable;
import java.io.IOException;

/**
 * 事务ACK发送，并切实可以刷新的
 *
 * @author yaoalong
 * @Date 2016年1月26日
 * @Email yaoalong@foxmail.com
 */
public class M2mSendAckRequestProcessor implements RequestProcessor, Flushable {
    private static final Logger LOG = LoggerFactory
            .getLogger(M2mSendAckRequestProcessor.class);

    M2mLearner learner;

    M2mSendAckRequestProcessor(M2mLearner peer) {
        this.learner = peer;
    }

    /**
     * 事务投票
     */
    public void processRequest(M2mRequest si) {
        if (si.type != OpCode.sync) {
            M2mQuorumPacket qp = new M2mQuorumPacket(M2mLeader.ACK,
                    si.m2mTxnHeader.getZxid(), null);
            try {
                learner.writePacket(qp, true);
            } catch (IOException e) {

                LOG.warn(
                        "Closing connection to leader, exception during packet send",
                        e);
                try {
                    if (!learner.sock.isClosed()) {
                        learner.sock.close();
                    }
                } catch (IOException e1) {
                    // Nothing to do, we are shutting things down, so an
                    // exception here is irrelevant

                    LOG.debug("Ignoring error closing the connection", e1);
                }
            }
        }
    }

    /**
     * 直接刷新
     */
    public void flush() throws IOException {
        try {
            learner.writePacket(null, true);
        } catch (IOException e) {
            LOG.warn(
                    "Closing connection to leader, exception during packet send",
                    e);
            try {
                if (!learner.sock.isClosed()) {
                    learner.sock.close();
                }
            } catch (IOException e1) {
                // Nothing to do, we are shutting things down, so an exception
                // here is irrelevant
                LOG.debug("Ignoring error closing the connection", e1);
            }
        }
    }

    public void shutdown() {
        // Nothing needed
    }

}
