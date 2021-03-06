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

import java.util.ArrayList;
import java.util.LinkedList;
public class M2mCommitProcessor extends Thread implements RequestProcessor {
    private static final Logger LOG = LoggerFactory
            .getLogger(M2mCommitProcessor.class);

/**
 * 请求保持直到commit 动作到来
 * 这里所有对外提供的方法都进行了对应的同步
 */
    /**
     * Requests that we are holding until the commit comes in.
     */
    LinkedList<M2mRequest> queuedRequests = new LinkedList<M2mRequest>();

    /**
     * Requests that have been committed.
     * 已经被commit的request
     */
    LinkedList<M2mRequest> committedRequests = new LinkedList<M2mRequest>();

    RequestProcessor nextProcessor;
    ArrayList<M2mRequest> toProcess = new ArrayList<M2mRequest>();

    /**
     * This flag indicates whether we need to wait for a response to come back
     * from the leader or we just let the sync operation flow through like a
     * read. The flag will be true if the CommitProcessor is in a Leader
     * pipeline.
     */
    boolean matchSyncs;
    volatile boolean finished = false;

    public M2mCommitProcessor(RequestProcessor nextProcessor, String id,
                              boolean matchSyncs) {
        super("CommitProcessor:" + id);
        this.nextProcessor = nextProcessor;
        this.matchSyncs = matchSyncs;
    }

    @Override
    public void run() {
        try {
            M2mRequest nextPending = null;
            while (!finished) {
                int len = toProcess.size();
                for (int i = 0; i < len; i++) {
                    nextProcessor.processRequest(toProcess.get(i));
                }
                toProcess.clear();//确保toProcess里面的都会被提前处理
                synchronized (this) {
                    /**
                     * 只有第一个处理过了，后面才能接着处理
                     */
                    if ((queuedRequests.size() == 0 || nextPending != null)
                            && committedRequests.size() == 0) {
                        wait();
                        continue;
                    }
                    // First check and see if the commit came in for the pending
                    // request
                    /**
                     * 检查等待队列，将commit的移交给toProcessor中
                     */
                    if ((queuedRequests.size() == 0 || nextPending != null)
                            && committedRequests.size() > 0) {
                        M2mRequest r = committedRequests.remove();
                        /*
                         * We match with nextPending so that we can move to the
                         * next request when it is committed. We also want to
                         * use nextPending because it has the cnxn member set
                         * properly.
                         */
                        if (nextPending != null

                                && nextPending.cxid == r.cxid) {
                            // we want to send our version of the request.
                            // the pointer to the connection in the request
                            nextPending.m2mTxnHeader = r.m2mTxnHeader;
                            nextPending.txn = r.txn;
                            nextPending.zxid = r.zxid;
                            toProcess.add(nextPending);
                            nextPending = null;
                        } else {
                            // this request came from someone else so just
                            // send the commit packet
                            toProcess.add(r);
                        }
                    }
                }

                // We haven't matched the pending requests, so go back to
                // waiting
                if (nextPending != null) {
                    continue;
                }

                synchronized (this) {
                    // Process the next requests in the queuedRequests
                    while (nextPending == null && queuedRequests.size() > 0) {
                        M2mRequest request = queuedRequests.remove();
                        switch (request.type) {
                            case OpCode.create:
                            case OpCode.delete:
                            case OpCode.setData:
                                nextPending = request;// 因为是事务类型的，所以需要挂起等待
                                break;
                            case OpCode.sync:
                                if (matchSyncs) {
                                    nextPending = request;
                                } else {
                                    toProcess.add(request);
                                }
                                break;
                            default:
                                toProcess.add(request); // 如果不是事务的，添加到toProcess队列中，下个循环可以直接提交个给FinalPorcessor
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            LOG.warn("Interrupted exception while waiting", e);
        } catch (Throwable e) {
            LOG.error("Unexpected exception causing CommitProcessor to exit", e);
        }
        LOG.info("CommitProcessor exited loop!");
    }

    synchronized public void commit(M2mRequest request) {
        System.out.println("commit :"+(System.nanoTime()-request.createTime));
        if (!finished) {
            if (request == null) {
                LOG.warn("Committed a null!", new Exception(
                        "committing a null! "));
                return;
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Committing request:: " + request);
            }
            committedRequests.add(request);
            notifyAll();
        }
    }

    synchronized public void processRequest(M2mRequest request) {

        System.out.println("commit processor:"+(System.nanoTime()-request.createTime));
        if (LOG.isDebugEnabled()) {
            LOG.debug("Processing request:: " + request);
        }

        if (!finished) {
            queuedRequests.add(request);
            notifyAll();
        }
    }

    public void shutdown() {
        LOG.info("Shutting down");
        synchronized (this) {
            finished = true;
            queuedRequests.clear();
            notifyAll();
        }
        if (nextProcessor != null) {
            nextProcessor.shutdown();
        }
    }

}
