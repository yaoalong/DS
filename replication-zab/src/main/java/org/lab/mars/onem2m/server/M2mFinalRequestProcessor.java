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

package org.lab.mars.onem2m.server;

import io.netty.channel.Channel;

import java.nio.ByteBuffer;

import lab.mars.ds.reflection.ResourceReflection;

import org.lab.mars.ds.server.M2mDataNode;
import org.lab.mars.ds.server.ProcessTxnResult;
import org.lab.mars.onem2m.KeeperException;
import org.lab.mars.onem2m.KeeperException.Code;
import org.lab.mars.onem2m.KeeperException.SessionMovedException;
import org.lab.mars.onem2m.M2mKeeperException;
import org.lab.mars.onem2m.ZooDefs.OpCode;
import org.lab.mars.onem2m.jute.M2mRecord;
import org.lab.mars.onem2m.proto.M2mCreateResponse;
import org.lab.mars.onem2m.proto.M2mGetDataRequest;
import org.lab.mars.onem2m.proto.M2mGetDataResponse;
import org.lab.mars.onem2m.proto.M2mPacket;
import org.lab.mars.onem2m.proto.M2mReplyHeader;
import org.lab.mars.onem2m.proto.M2mSetDataResponse;
import org.lab.mars.onem2m.server.ZooKeeperServer.ChangeRecord;
import org.lab.mars.onem2m.txn.M2mTxnHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Request processor actually applies any transaction associated with a
 * request and services any queries. It is always at the end of a
 * RequestProcessor chain (hence the name), so it does not have a nextProcessor
 * member.
 * <p>
 * This RequestProcessor counts on ZooKeeperServer to populate the
 * outstandingRequests member of ZooKeeperServer.
 */
public class M2mFinalRequestProcessor implements RequestProcessor {
    private static final Logger LOG = LoggerFactory
            .getLogger(M2mFinalRequestProcessor.class);

    ZooKeeperServer zks;

    public M2mFinalRequestProcessor(ZooKeeperServer zks) {
        this.zks = zks;
    }

    public void processRequest(M2mRequest request) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Processing request:: " + request);
        }
        ProcessTxnResult rc = null;
        synchronized (zks.outstandingChanges) {
            while (!zks.outstandingChanges.isEmpty()
                    && zks.outstandingChanges.get(0).zxid <= request.zxid) {
                ChangeRecord changeRecord = zks.outstandingChanges.remove(0);
                if (changeRecord.zxid < request.zxid) {
                    LOG.warn("Zxid outstanding " + changeRecord.zxid
                            + " is less than current " + request.zxid);
                }
            }
            if (request.m2mTxnHeader != null) {
                M2mTxnHeader hdr = request.m2mTxnHeader;
                M2mRecord txn = request.txn;
                hdr.setZxid(request.zxid);
                try {
                    rc = zks.processTxn(hdr, txn);
                } catch (M2mKeeperException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (M2mRequest.isQuorum(request.type)) {
                zks.getDSDatabase().addCommittedProposal(request);
            }
        }

        if (request.m2mTxnHeader != null) {
            ServerCnxnFactory scxn = zks.getServerCnxnFactory();
            if (scxn != null && request.channel == null) {
                return;
            }
        }

        if (request.channel == null) {
            return;
        }
        Channel channel = request.channel;

        zks.decInProcess();
        Code err = Code.OK;
        M2mRecord rsp = null;
        try {

            KeeperException ke = request.getException();
            if (ke != null) {
                throw ke;
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("{}", request);
            }
            switch (request.type) {
            case OpCode.create: {
                rsp = new M2mCreateResponse(rc.path);
                err = Code.get(rc.err);
                break;
            }
            case OpCode.delete: {
                err = Code.get(rc.err);
                break;
            }
            case OpCode.setData: {
                rsp = new M2mSetDataResponse();
                err = Code.get(rc.err);
                break;
            }
            case OpCode.getData: {
                M2mGetDataRequest getDataRequest = new M2mGetDataRequest();
                M2mByteBufferInputStream.byteBuffer2Record(request.request,
                        getDataRequest);
                M2mDataNode m2mDataNode = (M2mDataNode) zks.getDSDatabase()
                        .getData(getDataRequest.getPath());
                if (m2mDataNode != null) {
                    rsp = new M2mGetDataResponse(
                            ResourceReflection.serializeKryo(m2mDataNode));
                }
                break;
            }
            }
        } catch (SessionMovedException e) {
            return;
        } catch (KeeperException e) {
            err = e.code();
        } catch (Exception e) {
            LOG.error("Failed to process " + request, e);
            StringBuilder sb = new StringBuilder();
            ByteBuffer bb = request.request;
            bb.rewind();
            while (bb.hasRemaining()) {
                sb.append(Integer.toHexString(bb.get() & 0xff));
            }
            LOG.error("Dumping request buffer: 0x" + sb.toString());
            err = Code.MARSHALLINGERROR;
        }
        long lastZxid = 0;
        try {
            lastZxid = zks.getDSDatabase().getLastProcessedZxid();
        } catch (M2mKeeperException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        M2mReplyHeader hdr = new M2mReplyHeader(request.cxid, lastZxid,
                err.intValue());
        M2mPacket m2mPacket = new M2mPacket(null, hdr, null, rsp);
        System.out.println("发送");
        channel.writeAndFlush(m2mPacket);
    }

    public void shutdown() {
        LOG.info("shutdown of request processor complete");
    }

}
