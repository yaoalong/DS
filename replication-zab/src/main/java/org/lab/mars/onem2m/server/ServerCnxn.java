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

import org.lab.mars.onem2m.Watcher;
import org.lab.mars.onem2m.proto.RequestHeader;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Interface to a Server connection - represents a connection from a client
 * to the server.
 */
public abstract class ServerCnxn implements Stats, Watcher {
    // This is just an arbitrary object to represent requests issued by
    // (aka owned by) this class
    final public static Object me = new Object();
    /*
     * See <a href="{@docRoot}/../../../docs/zookeeperAdmin.html#sc_zkCommands">
     * Zk Admin</a>. this link is for all the commands.
     */
    protected final static int confCmd =
            ByteBuffer.wrap("conf".getBytes()).getInt();
    /*
     * See <a href="{@docRoot}/../../../docs/zookeeperAdmin.html#sc_zkCommands">
     * Zk Admin</a>. this link is for all the commands.
     */
    protected final static int consCmd =
            ByteBuffer.wrap("cons".getBytes()).getInt();
    /*
     * See <a href="{@docRoot}/../../../docs/zookeeperAdmin.html#sc_zkCommands">
     * Zk Admin</a>. this link is for all the commands.
     */
    protected final static int crstCmd =
            ByteBuffer.wrap("crst".getBytes()).getInt();
    /*
     * See <a href="{@docRoot}/../../../docs/zookeeperAdmin.html#sc_zkCommands">
     * Zk Admin</a>. this link is for all the commands.
     */
    protected final static int dumpCmd =
            ByteBuffer.wrap("dump".getBytes()).getInt();
    /*
     * See <a href="{@docRoot}/../../../docs/zookeeperAdmin.html#sc_zkCommands">
     * Zk Admin</a>. this link is for all the commands.
     */
    protected final static int enviCmd =
            ByteBuffer.wrap("envi".getBytes()).getInt();
    /*
     * See <a href="{@docRoot}/../../../docs/zookeeperAdmin.html#sc_zkCommands">
     * Zk Admin</a>. this link is for all the commands.
     */
    protected final static int getTraceMaskCmd =
            ByteBuffer.wrap("gtmk".getBytes()).getInt();
    /*
     * See <a href="{@docRoot}/../../../docs/zookeeperAdmin.html#sc_zkCommands">
     * Zk Admin</a>. this link is for all the commands.
     */
    protected final static int ruokCmd =
            ByteBuffer.wrap("ruok".getBytes()).getInt();
    /*
     * See <a href="{@docRoot}/../../../docs/zookeeperAdmin.html#sc_zkCommands">
     * Zk Admin</a>. this link is for all the commands.
     */
    protected final static int setTraceMaskCmd =
            ByteBuffer.wrap("stmk".getBytes()).getInt();
    /*
     * See <a href="{@docRoot}/../../../docs/zookeeperAdmin.html#sc_zkCommands">
     * Zk Admin</a>. this link is for all the commands.
     */
    protected final static int srvrCmd =
            ByteBuffer.wrap("srvr".getBytes()).getInt();
    /*
     * See <a href="{@docRoot}/../../../docs/zookeeperAdmin.html#sc_zkCommands">
     * Zk Admin</a>. this link is for all the commands.
     */
    protected final static int srstCmd =
            ByteBuffer.wrap("srst".getBytes()).getInt();
    /*
     * See <a href="{@docRoot}/../../../docs/zookeeperAdmin.html#sc_zkCommands">
     * Zk Admin</a>. this link is for all the commands.
     */
    protected final static int statCmd =
            ByteBuffer.wrap("stat".getBytes()).getInt();
    /*
     * See <a href="{@docRoot}/../../../docs/zookeeperAdmin.html#sc_zkCommands">
     * Zk Admin</a>. this link is for all the commands.
     */
    protected final static int wchcCmd =
            ByteBuffer.wrap("wchc".getBytes()).getInt();
    /*
     * See <a href="{@docRoot}/../../../docs/zookeeperAdmin.html#sc_zkCommands">
     * Zk Admin</a>. this link is for all the commands.
     */
    protected final static int wchpCmd =
            ByteBuffer.wrap("wchp".getBytes()).getInt();
    /*
     * See <a href="{@docRoot}/../../../docs/zookeeperAdmin.html#sc_zkCommands">
     * Zk Admin</a>. this link is for all the commands.
     */
    protected final static int wchsCmd =
            ByteBuffer.wrap("wchs".getBytes()).getInt();
    /*
     * See <a href="{@docRoot}/../../../docs/zookeeperAdmin.html#sc_zkCommands">
     * Zk Admin</a>. this link is for all the commands.
     */
    protected final static int mntrCmd = ByteBuffer.wrap("mntr".getBytes())
            .getInt();
    /*
     * See <a href="{@docRoot}/../../../docs/zookeeperAdmin.html#sc_zkCommands">
     * Zk Admin</a>. this link is for all the commands.
     */
    protected final static int isroCmd = ByteBuffer.wrap("isro".getBytes())
            .getInt();
    protected final static HashMap<Integer, String> cmd2String =
            new HashMap<Integer, String>();

    // specify all of the commands that are available
    static {
        cmd2String.put(confCmd, "conf");
        cmd2String.put(consCmd, "cons");
        cmd2String.put(crstCmd, "crst");
        cmd2String.put(dumpCmd, "dump");
        cmd2String.put(enviCmd, "envi");
        cmd2String.put(getTraceMaskCmd, "gtmk");
        cmd2String.put(ruokCmd, "ruok");
        cmd2String.put(setTraceMaskCmd, "stmk");
        cmd2String.put(srstCmd, "srst");
        cmd2String.put(srvrCmd, "srvr");
        cmd2String.put(statCmd, "stat");
        cmd2String.put(wchcCmd, "wchc");
        cmd2String.put(wchpCmd, "wchp");
        cmd2String.put(wchsCmd, "wchs");
        cmd2String.put(mntrCmd, "mntr");
        cmd2String.put(isroCmd, "isro");
    }

    protected final Date established = new Date();
    protected final AtomicLong packetsReceived = new AtomicLong();
    protected final AtomicLong packetsSent = new AtomicLong();
    protected long minLatency;
    protected long maxLatency;
    protected String lastOp;
    protected long lastCxid;
    protected long lastZxid;
    protected long lastResponseTime;
    protected long lastLatency;
    protected long count;
    protected long totalLatency;
    /**
     * If the client is of old version, we don't send r-o mode info to it.
     * The reason is that if we would, old C client doesn't read it, which
     * results in TCP RST packet, i.e. "connection reset by peer".
     */
    boolean isOldClient = true;

    abstract int getSessionTimeout();

    abstract void setSessionTimeout(int sessionTimeout);

    abstract void close();



    abstract long getSessionId();

    abstract void setSessionId(long sessionId);


    protected abstract ServerStats serverStats();

    public synchronized void resetStats() {
        packetsReceived.set(0);
        packetsSent.set(0);
        minLatency = Long.MAX_VALUE;
        maxLatency = 0;
        lastOp = "NA";
        lastCxid = -1;
        lastZxid = -1;
        lastResponseTime = 0;
        lastLatency = 0;

        count = 0;
        totalLatency = 0;
    }

    protected long incrPacketsReceived() {
        return packetsReceived.incrementAndGet();
    }

    protected void incrOutstandingRequests(RequestHeader h) {
    }

    protected long incrPacketsSent() {
        return packetsSent.incrementAndGet();
    }

    protected synchronized void updateStatsForResponse(long cxid, long zxid,
                                                       String op, long start, long end) {
        // don't overwrite with "special" xids - we're interested
        // in the clients last real operation
        if (cxid >= 0) {
            lastCxid = cxid;
        }
        lastZxid = zxid;
        lastOp = op;
        lastResponseTime = end;
        long elapsed = end - start;
        lastLatency = elapsed;
        if (elapsed < minLatency) {
            minLatency = elapsed;
        }
        if (elapsed > maxLatency) {
            maxLatency = elapsed;
        }
        count++;
        totalLatency += elapsed;
    }

    public Date getEstablished() {
        return (Date) established.clone();
    }

    public abstract long getOutstandingRequests();

    public long getPacketsReceived() {
        return packetsReceived.longValue();
    }

    public long getPacketsSent() {
        return packetsSent.longValue();
    }

    public synchronized long getMinLatency() {
        return minLatency == Long.MAX_VALUE ? 0 : minLatency;
    }

    public synchronized long getAvgLatency() {
        return count == 0 ? 0 : totalLatency / count;
    }

    public synchronized long getMaxLatency() {
        return maxLatency;
    }

    public synchronized String getLastOperation() {
        return lastOp;
    }

    public synchronized long getLastCxid() {
        return lastCxid;
    }

    public synchronized long getLastZxid() {
        return lastZxid;
    }

    public synchronized long getLastResponseTime() {
        return lastResponseTime;
    }

    public synchronized long getLastLatency() {
        return lastLatency;
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        PrintWriter pwriter = new PrintWriter(sw);
        pwriter.flush();
        pwriter.close();
        return sw.toString();
    }

    public abstract InetSocketAddress getRemoteSocketAddress();


    protected static class CloseRequestException extends IOException {
        private static final long serialVersionUID = -7854505709816442681L;

        public CloseRequestException(String msg) {
            super(msg);
        }
    }

    protected static class EndOfStreamException extends IOException {
        private static final long serialVersionUID = -8255690282104294178L;

        public EndOfStreamException(String msg) {
            super(msg);
        }

        public String toString() {
            return "EndOfStreamException: " + getMessage();
        }
    }

}
