package lab.mars.ds.persistence;

import lab.mars.ds.loadbalance.RangeDO;
import org.lab.mars.ds.server.M2mDataNode;
import org.lab.mars.ds.server.ProcessTxnResult;
import org.lab.mars.onem2m.M2mKeeperException;
import org.lab.mars.onem2m.jute.M2mRecord;
import org.lab.mars.onem2m.txn.M2mTxnHeader;

import java.util.List;

/**
 * Author:yaoalong.
 * Date:2016/3/3.
 * Email:yaoalong@foxmail.com
 */
public interface DSDatabaseInterface {
    M2mDataNode retrieve(String key)throws  M2mKeeperException;

    List<M2mDataNode> retrieve(Long zxid)throws  M2mKeeperException;

    Long create(Object object) throws M2mKeeperException;

    Long delete(String key)throws  M2mKeeperException;

    Long update(String key, M2mDataNode updated)throws  M2mKeeperException;

    boolean truncate(Long zxid)throws  M2mKeeperException;

    List<M2mDataNode> getCertainData(Long low, Long high);

    Long getMaxZxid(List<RangeDO> rangeDOs)throws  M2mKeeperException;

    void close();

    ProcessTxnResult processTxn(M2mTxnHeader header, M2mRecord m2mRecord);

}
