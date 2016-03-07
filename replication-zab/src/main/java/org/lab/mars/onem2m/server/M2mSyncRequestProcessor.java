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

package org.lab.mars.onem2m.server;

import java.io.Flushable;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This RequestProcessor logs requests to disk. It batches the requests to do
 * the io efficiently. The request is not passed to the next RequestProcessor
 * until its log has been synced to disk.
 *
 * SyncRequestProcessor is used in 3 different cases 1. Leader - Sync request to
 * disk and forward it to AckRequestProcessor which send ack back to itself. 2.
 * Follower - Sync request to disk and forward request to
 * SendAckRequestProcessor which send the packets to leader.
 * SendAckRequestProcessor is flushable which allow us to force push packets to
 * leader. 3. Observer - Sync committed request to disk (received as INFORM
 * packet). It never send ack back to the leader, so the nextProcessor will be
 * null. This change the semantic of txnlog on the observer since it only
 * contains committed txns.
 */
// TODO 现在没有利用到SyncRequestProcessor,需要添加
public class M2mSyncRequestProcessor extends Thread implements RequestProcessor {
    private static final Logger LOG = LoggerFactory
            .getLogger(M2mSyncRequestProcessor.class);
    private final LinkedBlockingQueue<M2mRequest> queuedRequests = new LinkedBlockingQueue<M2mRequest>();
    private final RequestProcessor nextProcessor;

    volatile private boolean running;

    /**
     * Transactions that have been written and are waiting to be flushed to
     * disk. Basically this is the list of SyncItems whose callbacks will be
     * invoked after flush returns successfully.
     */
    private final LinkedList<M2mRequest> toFlush = new LinkedList<M2mRequest>();
    /**
     * The number of log entries to log before starting a snapshot
     */
    private static int snapCount = ZooKeeperServer.getSnapCount();

    public M2mSyncRequestProcessor(ZooKeeperServer zks,
                                   RequestProcessor nextProcessor) {
        super("SyncThread:" + zks.getServerId());
        this.nextProcessor = nextProcessor;
        running = true;
    }

    /**
     * used by tests to check for changing snapcounts
     * 
     * @param count
     */
    public static void setSnapCount(int count) {
        snapCount = count;
    }

    /**
     * used by tests to get the snapcount
     * 
     * @return the snapcount
     */
    public static int getSnapCount() {
        return snapCount;
    }

    @Override
    public void run() {
        try {
            // we do this in an attempt to ensure that not all of the servers
            // in the ensemble take a snapshot at the same time
            while (true) {
                M2mRequest si = null;
                if (toFlush.isEmpty()) {
                    si = queuedRequests.take();
                } else {
                    si = queuedRequests.poll();
                    if (si == null) {
                        flush(toFlush);
                        continue;
                    }
                }
                if (si != null) {
                    // track the number of records written to the log
                    if (toFlush.isEmpty()) {
                        // optimization for read heavy workloads
                        // iff this is a read, and there are no pending
                        // flushes (writes), then just pass this to the next
                        // processor
                        if (nextProcessor != null) {
                            nextProcessor.processRequest(si);
                            if (nextProcessor instanceof Flushable) {
                                ((Flushable) nextProcessor).flush();
                            }
                        }
                        continue;
                    }
                    toFlush.add(si);
                    if (toFlush.size() > 1000) {
                        flush(toFlush);
                    }
                }
            }
        } catch (Throwable t) {
            LOG.error("Severe unrecoverable error, exiting", t);
            running = false;
            System.exit(11);
        }
        LOG.info("SyncRequestProcessor exited!");
    }

    private void flush(LinkedList<M2mRequest> toFlush) throws IOException,
            RequestProcessorException {
        if (toFlush.isEmpty())
            return;

        // zks.getZKDatabase().commit();
        while (!toFlush.isEmpty()) {
            M2mRequest i = toFlush.remove();
            if (nextProcessor != null) {
                nextProcessor.processRequest(i);
            }
        }
        if (nextProcessor != null && nextProcessor instanceof Flushable) {
            ((Flushable) nextProcessor).flush();
        }
    }

    public void shutdown() {
        LOG.info("Shutting down");
        // queuedRequests.add(requestOfDeath);

        if (nextProcessor != null) {
            nextProcessor.shutdown();
        }
    }

    public void processRequest(M2mRequest request) {
        // request.addRQRec(">sync");
        queuedRequests.add(request);
    }

}
