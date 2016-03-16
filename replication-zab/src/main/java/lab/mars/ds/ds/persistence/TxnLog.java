package lab.mars.ds.ds.persistence;

import org.lab.mars.onem2m.jute.M2mRecord;
import org.lab.mars.onem2m.txn.M2mTxnHeader;

import java.io.IOException;

/**
 * Author:yaoalong.
 * Date:2016/3/15.
 * Email:yaoalong@foxmail.com
 */
public interface TxnLog {

    /**
     * roll the current
     * log being appended to
     * @throws IOException
     */
    void rollLog() throws IOException;
    /**
     * Append a request to the transaction log
     * @param hdr the transaction header
     * @param r the transaction itself
     * returns true iff something appended, otw false
     * @throws IOException
     */
    boolean append(M2mTxnHeader hdr, M2mRecord r) throws IOException;

    /**
     * Start reading the transaction logs
     * from a given zxid
     * @param zxid
     * @return returns an iterator to read the
     * next transaction in the logs.
     * @throws IOException
     */
    TxnIterator read(long zxid) throws IOException;

    /**
     * the last zxid of the logged transactions.
     * @return the last zxid of the logged transactions.
     * @throws IOException
     */
    long getLastLoggedZxid() throws IOException;

    /**
     * truncate the log to get in sync with the
     * leader.
     * @param zxid the zxid to truncate at.
     * @throws IOException
     */
    boolean truncate(long zxid) throws IOException;

    /**
     * the dbid for this transaction log.
     * @return the dbid for this transaction log.
     * @throws IOException
     */
    long getDbId() throws IOException;

    /**
     * commmit the trasaction and make sure
     * they are persisted
     * @throws IOException
     */
    void commit() throws IOException;

    /**
     * close the transactions logs
     */
    void close() throws IOException;
    /**
     * an iterating interface for reading
     * transaction logs.
     */
    public interface TxnIterator {
        /**
         * return the transaction header.
         * @return return the transaction header.
         */
        M2mTxnHeader getHeader();

        /**
         * return the transaction record.
         * @return return the transaction record.
         */
        M2mRecord getTxn();

        /**
         * go to the next transaction record.
         * @throws IOException
         */
        boolean next() throws IOException;

        /**
         * close files and release the
         * resources
         * @throws IOException
         */
        void close() throws IOException;
    }
}
