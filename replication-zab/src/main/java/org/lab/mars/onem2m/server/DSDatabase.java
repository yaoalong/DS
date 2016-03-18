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

import lab.mars.ds.ds.persistence.FileTxnLog;
import lab.mars.ds.ds.persistence.PlayBackListener;
import lab.mars.ds.loadbalance.RangeDO;
import lab.mars.ds.persistence.DSDatabaseInterface;
import org.lab.mars.ds.server.M2mDataNode;
import org.lab.mars.ds.server.ProcessTxnResult;
import org.lab.mars.onem2m.M2mKeeperException;
import org.lab.mars.onem2m.jute.M2mBinaryOutputArchive;
import org.lab.mars.onem2m.jute.M2mInputArchive;
import org.lab.mars.onem2m.jute.M2mOutputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;
import org.lab.mars.onem2m.server.quorum.M2mLeader;
import org.lab.mars.onem2m.server.quorum.M2mLeader.Proposal;
import org.lab.mars.onem2m.server.quorum.M2mQuorumPacket;
import org.lab.mars.onem2m.server.util.SerializeUtils;
import org.lab.mars.onem2m.txn.M2mTxnHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

/**
 * This class maintains the in memory database of zookeeper server states that
 * includes the sessions, datatree and the committed logs. It is booted up after
 * reading the logs and snapshots from the disk.
 */
/*
 * 连接数据库以及内存数据
 */
public class DSDatabase implements M2mRecord {

    public static final int commitLogCount = 500; // 临时会存储500个
    /**
     *
     */
    private static final long serialVersionUID = -2369189189754653975L;
    private static final Logger LOG = LoggerFactory.getLogger(DSDatabase.class);
    private static final ConcurrentHashMap<String, M2mDataNode> nodes = new ConcurrentHashMap<>();
    // protected FileTxnSnapLog snapLog;
    protected long minCommittedLog, maxCommittedLog;
    protected LinkedList<Proposal> committedLog = new LinkedList<Proposal>();// 提交的事务
    protected ReentrantReadWriteLock logLock = new ReentrantReadWriteLock();
    private DSDatabaseInterface m2mDataBase;
    volatile private boolean initialized = false;
    private volatile long lastProcessedZxid = 0;

    private List<RangeDO> rangeDOs;
    /**
     * 事务日志类
     */
    private FileTxnLog fileTxnLog;

    /**
     * 初始化的同时进行数据加载
     */
    public DSDatabase(DSDatabaseInterface m2mDataBase, FileTxnLog fileTxnLog) {
        this.m2mDataBase = m2mDataBase;
        this.fileTxnLog = fileTxnLog;
    }

    /**
     * checks to see if the zk database has been initialized or not.
     *
     * @return true if zk database is initialized and false if not
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * clear the zkdatabase. Note to developers - be careful to see that the
     * clear method does clear out all the data structures in zkdatabase.
     */
    public void clear() {
        minCommittedLog = 0;
        maxCommittedLog = 0;
        WriteLock lock = logLock.writeLock();
        try {
            lock.lock();
            committedLog.clear();
        } finally {
            lock.unlock();
        }
        initialized = false;
    }

    /**
     * Get the lock that controls the committedLog. If you want to get the
     * pointer to the committedLog, you need to use this lock to acquire a read
     * lock before calling getCommittedLog()
     *
     * @return the lock that controls the committed log
     */
    // public ReentrantReadWriteLock getLogLock() {
    // return logLock;
    // }
    //
    public synchronized LinkedList<Proposal> getCommittedLog() {
        ReadLock rl = logLock.readLock();
        // only make a copy if this thread isn't already holding a lock
        if (logLock.getReadHoldCount() <= 0) {
            try {
                rl.lock();
                return new LinkedList<Proposal>(this.committedLog);
            } finally {
                rl.unlock();
            }
        }
        return this.committedLog;
    }

    /**
     * get the last processed zxid from a datatree
     *
     * @return the last processed zxid of a datatree
     */

    /**
     * load the database from the disk onto memory and also add the transactions
     * to the committedlog in memory.
     *
     * @return the last valid zxid on disk
     * @throws IOException
     */
    public long loadDataBase() throws IOException {
        PlayBackListener listener = new PlayBackListener() {
            public void onTxnLoaded(M2mTxnHeader hdr, M2mRecord txn) {
                M2mRequest r = new M2mRequest(null, 0, hdr.getType(), null);
                r.txn = txn;
                r.m2mTxnHeader = hdr;
                r.zxid = hdr.getZxid();
                addCommittedProposal(r);
            }
        };
        restore(listener);
        getLastProcessedZxid();
        initialized = true;
        return lastProcessedZxid;
    }

    public long restore(PlayBackListener playBackListener) throws IOException {
        return 0L;
        // TxnLog.TxnIterator itr = fileTxnLog.read(lastProcessedZxid + 1);
        // long highestZxid = lastProcessedZxid;
        // M2mTxnHeader hdr;
        // try {
        // while (true) {
        // // iterator points to
        // // the first valid txn when initialized
        // hdr = itr.getHeader();
        // if (hdr == null) {
        // // empty logs
        // return lastProcessedZxid;
        // }
        // if (hdr.getZxid() < highestZxid && highestZxid != 0) {
        // LOG.error(
        // "{}(higestZxid) > {}(next log) for type {}",
        // new Object[]{highestZxid, hdr.getZxid(),
        // hdr.getType()});
        // } else {
        // highestZxid = hdr.getZxid();
        // }
        //
        // processTxn(hdr, itr.getTxn());// 处理具体的事务
        //
        // playBackListener.onTxnLoaded(hdr, itr.getTxn());
        // if (!itr.next())
        // break;
        // }
        // } finally {
        // if (itr != null) {
        // itr.close();
        // }
        // }
        // return highestZxid;
    }

    public Long getLastProcessedZxid() {
        lastProcessedZxid = m2mDataBase.getMaxZxid(rangeDOs);
        return lastProcessedZxid;
    }

    public void setLastProcessedZxid(Long lastProcessedZxid) {
        this.lastProcessedZxid = lastProcessedZxid;
    }

    /**
     * maintains a list of last <i>committedLog</i> or so committed requests.
     * This is used for fast follower synchronization.
     *
     * @param request committed request
     */
    public void addCommittedProposal(M2mRequest request) {
        WriteLock wl = logLock.writeLock();
        try {
            wl.lock();
            if (committedLog.size() > commitLogCount) {
                committedLog.removeFirst();
                minCommittedLog = committedLog.getFirst().packet.getZxid();
            }
            if (committedLog.size() == 0) {
                minCommittedLog = request.zxid;
                maxCommittedLog = request.zxid;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            M2mBinaryOutputArchive boa = M2mBinaryOutputArchive
                    .getArchive(baos);
            try {
                request.m2mTxnHeader.serialize(boa, "hdr");
                if (request.txn != null) {
                    request.txn.serialize(boa, "txn");
                }
                baos.close();
            } catch (IOException e) {
                LOG.error("This really should be impossible", e);
            }
            M2mQuorumPacket pp = new M2mQuorumPacket(M2mLeader.PROPOSAL,
                    request.zxid, baos.toByteArray());
            Proposal p = new Proposal();
            p.packet = pp;
            p.m2mRequest = request;
            committedLog.add(p);
            maxCommittedLog = p.packet.getZxid();
        } finally {
            wl.unlock();
        }
    }

    /**
     * return nodes count
     *
     * @return
     */
    public int getNodeCount() {
        return nodes.size();
    }

    /**
     * the process txn on the data
     *
     * @param hdr the txnheader for the txn
     * @param txn the transaction that needs to be processed
     * @return the result of processing the transaction on this
     * datatree/zkdatabase
     */
    /*
     * m2m内存数据库处理事务请求
     */
    public ProcessTxnResult processTxn(M2mTxnHeader hdr, M2mRecord txn) {
        ProcessTxnResult processTxnResult = m2mDataBase.processTxn(hdr, txn);
        if (processTxnResult.zxid > getLastProcessedZxid()) {
            setLastProcessedZxid(processTxnResult.zxid);
        }
        return processTxnResult;
    }

    /*
     * 获取数据
     */
    public Object getData(String key) {
        return m2mDataBase.retrieve(key);
    }

    // /**
    // * Truncate the ZKDatabase to the specified zxid
    // * @param zxid the zxid to truncate zk database to
    // * @return true if the truncate is successful and false if not
    // * @throws IOException
    // */
    /*
     *
	 */
    public boolean truncateLog(long zxid) throws IOException {
        clear();
        if (!fileTxnLog.truncate(zxid)) {
            return false;
        }
        loadDataBase();
        return true;
    }

    /**
     * deserialize a snapshot from an input archive
     *
     * @param ia the input archive you want to deserialize from
     * @throws IOException
     */
    public void deserializeSnapshot(M2mInputArchive ia) throws IOException {
        clear();
        SerializeUtils.deserializeSnapshot(this, ia);
        initialized = true;
    }

    /**
     * serialize the snapshot
     *
     * @param oa the output archive to which the snapshot needs to be
     *           serialized
     * @throws IOException
     * @throws InterruptedException
     */
    public void serializeSnapshot(Long peerLast, M2mOutputArchive oa)
            throws IOException, InterruptedException {
        SerializeUtils.serializeSnapshot(peerLast, this, oa);
    }

    /**
     * 提交commit log
     *
     * @throws IOException
     */
    public void commit() throws IOException {
        fileTxnLog.commit();

    }

    /**
     * append to the underlying transaction log
     *
     * @param si the request to append
     * @return true if the append was succesfull and false if not
     */
    public boolean append(M2mRequest si) throws IOException {
        return this.fileTxnLog.append(si.m2mTxnHeader, si.txn);
    }

    /**
     * roll the underlying log
     */
    public void rollLog() throws IOException {
        fileTxnLog.rollLog();
    }

    /**
     * 关闭事务日志资源
     */
    public void close() throws IOException {
        this.fileTxnLog.close();
    }

    public long getMinCommittedLog() {
        return minCommittedLog;
    }

    public long getMaxCommittedLog() {
        return maxCommittedLog;
    }

    public ReentrantReadWriteLock getLogLock() {
        return logLock;
    }

    public void setlastProcessedZxid(long zxid) {

    }

    public void setRangeDOs(List<RangeDO> rangeDOs) {
        this.rangeDOs = rangeDOs;
    }

    public void serialize(Long peerLast, M2mOutputArchive archive, String tag)
            throws IOException {
        List<M2mDataNode> dataNodes = m2mDataBase.retrieve(peerLast);
        archive.writeInt(dataNodes.size(), "count");
        for (M2mDataNode m2mDataNode : dataNodes) {
            archive.writeString(m2mDataNode.getId(), "key");
            archive.writeRecord(m2mDataNode, "m2mDataNode");
        }

    }

    @Override
    public void deserialize(M2mInputArchive archive, String tag)
            throws IOException {
        int count = archive.readInt("count");
        while (count > 0) {
            M2mDataNode m2mDataNode = new M2mDataNode();
            String key = archive.readString("key");
            archive.readRecord(m2mDataNode, "m2mDataNode");
            nodes.put(key, m2mDataNode);
            try {
                m2mDataBase.create(m2mDataNode);
            } catch (M2mKeeperException.NodeExistsException e) {
            }
            count--;
        }
    }

    @Override
    public void serialize(M2mOutputArchive archive, String tag)
            throws IOException {
        archive.writeInt(nodes.size(), "count");
        for (Map.Entry<String, M2mDataNode> m2mDataNode : nodes.entrySet()) {
            archive.writeString(m2mDataNode.getKey(), "key");
            archive.writeRecord(m2mDataNode.getValue(), "m2mDataNode");
        }

    }
}
