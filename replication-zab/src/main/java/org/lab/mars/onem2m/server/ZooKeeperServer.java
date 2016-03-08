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

import io.netty.channel.ChannelHandlerContext;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.lab.mars.ds.server.DataTree.ProcessTxnResult;
import org.lab.mars.onem2m.data.StatPersisted;
import org.lab.mars.onem2m.jute.M2mBinaryOutputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;
import org.lab.mars.onem2m.proto.M2mPacket;
import org.lab.mars.onem2m.proto.M2mRequestHeader;
import org.lab.mars.onem2m.server.RequestProcessor.RequestProcessorException;
import org.lab.mars.onem2m.server.SessionTracker.Session;
import org.lab.mars.onem2m.server.SessionTracker.SessionExpirer;
import org.lab.mars.onem2m.txn.M2mTxnHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements a simple standalone ZooKeeperServer. It sets up the
 * following chain of RequestProcessors to process requests:
 * PrepRequestProcessor -> SyncRequestProcessor -> FinalRequestProcessor
 */
public class ZooKeeperServer implements SessionExpirer, ServerStats.Provider {
    public static final int DEFAULT_TICK_TIME = 3000;
    public final static Exception ok = new Exception("No prob");
    protected static final Logger LOG;
    /**
     * This is the secret that we use to generate passwords, for the moment it
     * is more of a sanity check.
     */
    static final private long superSecret = 0XB3415C00L;

    static {
        LOG = LoggerFactory.getLogger(ZooKeeperServer.class);
    }

    final List<ChangeRecord> outstandingChanges = new ArrayList<ChangeRecord>();
    // this data structure must be accessed under the outstandingChanges lock
    final HashMap<String, ChangeRecord> outstandingChangesForPath = new HashMap<String, ChangeRecord>();
    private final ServerStats serverStats;
    protected int tickTime = DEFAULT_TICK_TIME;
    /**
     * value of -1 indicates unset, use default
     */
    protected int minSessionTimeout = -1;
    /**
     * value of -1 indicates unset, use default
     */
    protected int maxSessionTimeout = -1;
    protected SessionTracker sessionTracker;
    protected long hzxid = 0;
    protected RequestProcessor firstProcessor;
    protected volatile boolean running;
    int requestsInProcess;
    private DSDatabase zkDb;
    private ServerCnxnFactory serverCnxnFactory;

    /**
     * Creates a ZooKeeperServer instance. Nothing is setup, use the setX
     * methods to prepare the instance (eg datadir, datalogdir, ticktime,
     * builder, etc...)
     *
     * @throws IOException
     */

    public ZooKeeperServer(int tickTime, int minSessionTimeout,
            int maxSessionTimeout, DSDatabase zkDb) {
        serverStats = new ServerStats(this);
        this.zkDb = zkDb;
        this.tickTime = tickTime;
        this.minSessionTimeout = minSessionTimeout;
        this.maxSessionTimeout = maxSessionTimeout;

        LOG.info("Created server with tickTime " + tickTime
                + " minSessionTimeout " + getMinSessionTimeout()
                + " maxSessionTimeout " + getMaxSessionTimeout());
    }

    /**
     *
     * @param tickTime
     * @throws IOException
     */
    public ZooKeeperServer(int tickTime) throws IOException {
        this(tickTime, -1, -1, new DSDatabase(null, null, null));
    }

    /**
     * This constructor is for backward compatibility with the existing unit
     * test code. It defaults to FileLogProvider persistence provider.
     */
    public ZooKeeperServer(File snapDir, File logDir, int tickTime)
            throws IOException {
        this(tickTime);
    }

    /**
     * Default constructor, relies on the config for its agrument values
     *
     * @throws IOException
     */
    public ZooKeeperServer() throws IOException {
        this(DEFAULT_TICK_TIME, -1, -1, new DSDatabase(null, null, null));
    }

    public static int getSnapCount() {
        String sc = System.getProperty("zookeeper.snapCount");
        try {
            int snapCount = Integer.parseInt(sc);

            // snapCount must be 2 or more. See
            // org.apache.zookeeper.server.SyncRequestProcessor
            if (snapCount < 2) {
                LOG.warn("SnapCount should be 2 or more. Now, snapCount is reset to 2");
                snapCount = 2;
            }
            return snapCount;
        } catch (Exception e) {
            return 100000;
        }
    }

    public ServerStats serverStats() {
        return serverStats;
    }

    public void dumpConf(PrintWriter pwriter) {
        pwriter.print("clientPort=");
        pwriter.println(getClientPort());
        pwriter.print("dataDir=");
        pwriter.print("dataLogDir=");
        pwriter.print("tickTime=");
        pwriter.println(getTickTime());
        pwriter.print("maxClientCnxns=");
        pwriter.println(serverCnxnFactory.getMaxClientCnxnsPerHost());
        pwriter.print("minSessionTimeout=");
        pwriter.println(getMinSessionTimeout());
        pwriter.print("maxSessionTimeout=");
        pwriter.println(getMaxSessionTimeout());

        pwriter.print("serverId=");
        pwriter.println(getServerId());
    }

    /**
     * get the zookeeper database for this server
     *
     * @return the zookeeper database for this server
     */
    public DSDatabase getZKDatabase() {
        return this.zkDb;
    }

    /**
     * set the zkdatabase for this zookeeper server
     *
     * @param zkDb
     */
    public void setZKDatabase(DSDatabase zkDb) {
        this.zkDb = zkDb;
    }

    /**
     * Restore sessions and data
     */
    public void loadData() throws IOException, InterruptedException {
        if (zkDb.isInitialized()) {
            setZxid(zkDb.getLastProcessedZxid());
        } else {
            setZxid(zkDb.loadDataBase());
        }

    }

    public void takeSnapshot() {

        // try {
        // txnLogFactory.save(zkDb.getDataTree(),
        // zkDb.getSessionWithTimeOuts());
        // } catch (IOException e) {
        // LOG.error("Severe unrecoverable error, exiting", e);
        // // This is a severe error that we cannot recover from,
        // // so we need to exit
        // System.exit(10);
        // }
    }

    /**
     * This should be called from a synchronized block on this!
     */
    synchronized public long getZxid() {
        return hzxid;
    }

    synchronized public void setZxid(long zxid) {
        hzxid = zxid;
    }

    synchronized long getNextZxid() {
        return ++hzxid;
    }

    long getTime() {
        return System.currentTimeMillis();
    }

    protected void killSession(long sessionId, long zxid) {
    }

    // private void close(long sessionId) {
    // submitRequest(null, sessionId, OpCode.closeSession, 0, null, null);
    // }

    // public void closeSession(long sessionId) {
    // LOG.info("Closing session 0x" + Long.toHexString(sessionId));
    //
    // // we do not want to wait for a session close. send it as soon as we
    // // detect it!
    // close(sessionId);
    // }

    void touch(ServerCnxn cnxn) throws MissingSessionException {
        if (cnxn == null) {
            return;
        }
        long id = cnxn.getSessionId();
        int to = cnxn.getSessionTimeout();
        if (!sessionTracker.touchSession(id, to)) {
            throw new MissingSessionException("No session with sessionid 0x"
                    + Long.toHexString(id)
                    + " exists, probably expired and removed");
        }
    }

    protected void registerJMX() {
    }

    public void startdata() throws IOException, InterruptedException {
        // check to see if zkDb is not null
        if (zkDb == null) {
            zkDb = new DSDatabase(null, null, null);
        }
        if (!zkDb.isInitialized()) {
            loadData();
        }
    }

    public void startup() {
        if (sessionTracker == null) {
            createSessionTracker();
        }
        startSessionTracker();
        setupRequestProcessors();

        registerJMX();

        synchronized (this) {
            running = true;
            notifyAll();
        }
    }

    protected void setupRequestProcessors() {
        RequestProcessor finalProcessor = new M2mFinalRequestProcessor(this);
        RequestProcessor syncProcessor = new M2mSyncRequestProcessor(this,
                finalProcessor);
        ((M2mSyncRequestProcessor) syncProcessor).start();
        firstProcessor = new PrepRequestProcessor(this, syncProcessor);
        ((PrepRequestProcessor) firstProcessor).start();
    }

    protected void createSessionTracker() {

    }

    protected void startSessionTracker() {
        ((SessionTrackerImpl) sessionTracker).start();
    }

    public boolean isRunning() {
        return running;
    }

    public void shutdown() {
        LOG.info("shutting down");

        this.running = false;
        if (sessionTracker != null) {
            sessionTracker.shutdown();
        }
        if (firstProcessor != null) {
            firstProcessor.shutdown();
        }
        if (zkDb != null) {
            zkDb.clear();
        }

    }

    protected void unregisterJMX() {

    }

    synchronized public void incInProcess() {
        requestsInProcess++;
    }

    synchronized public void decInProcess() {
        requestsInProcess--;
    }

    public int getInProcess() {
        return requestsInProcess;
    }

    byte[] generatePasswd(long id) {
        Random r = new Random(id ^ superSecret);
        byte p[] = new byte[16];
        r.nextBytes(p);
        return p;
    }

    protected void revalidateSession(ServerCnxn cnxn, long sessionId,
            int sessionTimeout) throws IOException {
        boolean rc = sessionTracker.touchSession(sessionId, sessionTimeout);
        if (LOG.isTraceEnabled()) {
            ZooTrace.logTraceMessage(LOG, ZooTrace.SESSION_TRACE_MASK,
                    "Session 0x" + Long.toHexString(sessionId) + " is valid: "
                            + rc);
        }
        finishSessionInit(cnxn, rc);
    }

    public void finishSessionInit(ServerCnxn cnxn, boolean valid) {
        // register with JMX

    }

    public long getServerId() {
        return 0;
    }

    public void submitRequest(M2mRequest si) {
        if (firstProcessor == null) {
            synchronized (this) {
                try {
                    while (!running) {
                        wait(1000);
                    }
                } catch (InterruptedException e) {
                    LOG.warn("Unexpected interruption", e);
                }
                if (firstProcessor == null) {
                    throw new RuntimeException("Not started");
                }
            }
        }
        try {
            boolean validpacket = Request.isValid(si.type);// 判断请求是否是支持的type,直接交给firstPorcessor
            if (validpacket) {
                firstProcessor.processRequest(si);
            } else {
                LOG.warn("Received packet at server of unknown type " + si.type);
            }
        } catch (RequestProcessorException e) {
            LOG.error("Unable to process request:" + e.getMessage(), e);
        }
    }

    public int getGlobalOutstandingLimit() {
        String sc = System.getProperty("zookeeper.globalOutstandingLimit");
        int limit;
        try {
            limit = Integer.parseInt(sc);
        } catch (Exception e) {
            limit = 1000;
        }
        return limit;
    }

    public ServerCnxnFactory getServerCnxnFactory() {
        return serverCnxnFactory;
    }

    public void setServerCnxnFactory(ServerCnxnFactory factory) {
        serverCnxnFactory = factory;
    }

    /**
     * return the last proceesed id from the datatree
     */
    public long getLastProcessedZxid() {
        return zkDb.getLastProcessedZxid();
    }

    /**
     * return the outstanding requests in the queue, which havent been processed
     * yet
     */
    public long getOutstandingRequests() {
        return getInProcess();
    }

    /**
     * trunccate the log to get in sync with others if in a quorum
     *
     * @param zxid
     *            the zxid that it needs to get in sync with others
     * @throws IOException
     */
    /*
     * TODO回滚操作
     */
    public void truncateLog(long zxid) throws IOException {
        // this.zkDb.truncateLog(zxid);
    }

    public int getTickTime() {
        return tickTime;
    }

    public void setTickTime(int tickTime) {
        LOG.info("tickTime set to " + tickTime);
        this.tickTime = tickTime;
    }

    public int getMinSessionTimeout() {
        return minSessionTimeout == -1 ? tickTime * 2 : minSessionTimeout;
    }

    public void setMinSessionTimeout(int min) {
        LOG.info("minSessionTimeout set to " + min);
        this.minSessionTimeout = min;
    }

    public int getMaxSessionTimeout() {
        return maxSessionTimeout == -1 ? tickTime * 20 : maxSessionTimeout;
    }

    public void setMaxSessionTimeout(int max) {
        LOG.info("maxSessionTimeout set to " + max);
        this.maxSessionTimeout = max;
    }

    public int getClientPort() {
        return serverCnxnFactory != null ? serverCnxnFactory.getLocalPort()
                : -1;
    }

    public String getState() {
        return "standalone";
    }

    /**
     * return the total number of client connections that are alive to this
     * server
     */
    public int getNumAliveConnections() {
        return serverCnxnFactory.getNumAliveConnections();
    }

    /*
     * 将请求发送给processor
     */
    public void processPacket(ChannelHandlerContext ctx, M2mPacket m2mPacket) {
        M2mRequestHeader m2mRequestHeader = m2mPacket.getM2mRequestHeader();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        M2mBinaryOutputArchive boa = M2mBinaryOutputArchive.getArchive(baos);
        try {
            m2mPacket.getRequest().serialize(boa, "request");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        M2mRequest m2mRequest = new M2mRequest(ctx.channel(),
                m2mRequestHeader.getXid(), m2mRequestHeader.getType(),
                ByteBuffer.wrap(baos.toByteArray()));
        submitRequest(m2mRequest);
    }

    /*
     * 在这里处理事务请求,应用到数据数据库
     */
    public ProcessTxnResult processTxn(M2mTxnHeader hdr, M2mRecord txn) {
        ProcessTxnResult rc;
        rc = getZKDatabase().processTxn(hdr, txn);
        return rc;
    }

    @Override
    public void expire(Session session) {

    }

    public static class MissingSessionException extends IOException {
        private static final long serialVersionUID = 7467414635467261007L;

        public MissingSessionException(String msg) {
            super(msg);
        }
    }

    /**
     * This structure is used to facilitate information sharing between PrepRP
     * and FinalRP.
     */
    static class ChangeRecord {
        long zxid;
        String path;
        StatPersisted stat; /* Make sure to create a new object when changing */
        int childCount;

        ChangeRecord(long zxid, String path, StatPersisted stat, int childCount) {
            this.zxid = zxid;
            this.path = path;
            this.stat = stat;
            this.childCount = childCount;
        }

        ChangeRecord duplicate(long zxid) {
            StatPersisted stat = new StatPersisted();
            return new ChangeRecord(zxid, path, stat, childCount);
        }
    }

}
