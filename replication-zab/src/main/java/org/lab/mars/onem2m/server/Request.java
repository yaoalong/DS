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

import io.netty.channel.ChannelHandlerContext;

import java.nio.ByteBuffer;

import org.lab.mars.onem2m.KeeperException;
import org.lab.mars.onem2m.ZooDefs.OpCode;
import org.lab.mars.onem2m.jute.M2mRecord;

/**
 * This is the structure that represents a request moving through a chain of
 * RequestProcessors. There are various pieces of information that is tacked
 * onto the request as it is processed.
 */
public class Request {
    public final static Request requestOfDeath = new Request(null, 0, 0, 0,
            null);

    public Request(ChannelHandlerContext ctx, long sessionId, int xid,
            int type, ByteBuffer bb) {
        this.ctx = ctx;
        this.sessionId = sessionId;
        this.cxid = xid;
        this.type = type;
        this.request = bb;
    }

    public final long sessionId;

    public final int cxid;

    public final int type;

    public final ByteBuffer request;

    public ChannelHandlerContext ctx;

    public M2mRecord txn;

    public long zxid = -1;

    public final long createTime = System.currentTimeMillis();

    private Object owner;

    private KeeperException e;

    public Object getOwner() {
        return owner;
    }

    public void setOwner(Object owner) {
        this.owner = owner;
    }

    /**
     * is the packet type a valid packet in zookeeper
     * 
     * @param type
     *            the type of the packet
     * @return true if a valid packet, false if not
     */
    static boolean isValid(int type) {
        // make sure this is always synchronized with Zoodefs!!
        switch (type) {
        case OpCode.notification:
            return false;
        case OpCode.create:
        case OpCode.delete:
        case OpCode.getData:
        case OpCode.setData:
        case OpCode.sync:
            return true;
        default:
            return false;
        }
    }

    static boolean isQuorum(int type) {
        switch (type) {
        case OpCode.getData:
            return false;
        case OpCode.error:
        case OpCode.create:
        case OpCode.delete:
        case OpCode.setData:
            return true;
        default:
            return false;
        }
    }

    static String op2String(int op) {
        switch (op) {
        case OpCode.notification:
            return "notification";
        case OpCode.create:
            return "create";
        case OpCode.delete:
            return "delete";
        case OpCode.getData:
            return "getData";

        case OpCode.setData:
            return "setData";
        case OpCode.sync:
            return "sync:";

        case OpCode.error:
            return "error";
        default:
            return "unknown " + op;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // best effort to print the path assoc with this request
        String path = "n/a";
        if (request != null && request.remaining() >= 4) {
            try {
                // make sure we don't mess with request itself
                ByteBuffer rbuf = request.asReadOnlyBuffer();
                rbuf.clear();
                int pathLen = rbuf.getInt();
                // sanity check
                if (pathLen >= 0 && pathLen < 4096
                        && rbuf.remaining() >= pathLen) {
                    byte b[] = new byte[pathLen];
                    rbuf.get(b);
                    path = new String(b);
                }
            } catch (Exception e) {
                // ignore - can't find the path, will output "n/a" instead
            }
        }
        sb.append(" reqpath:").append(path);

        return sb.toString();
    }

    public void setException(KeeperException e) {
        this.e = e;
    }

    public KeeperException getException() {
        return e;
    }
}
