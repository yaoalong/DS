package lab.mars.ds.persistence;

import java.util.List;

import lab.mars.ds.loadbalance.RangeDO;

import org.lab.mars.ds.server.M2mDataNode;
import org.lab.mars.ds.server.ProcessTxnResult;
import org.lab.mars.onem2m.M2mKeeperException;
import org.lab.mars.onem2m.jute.M2mRecord;
import org.lab.mars.onem2m.txn.M2mTxnHeader;

/**
 * Author:yaoalong. Date:2016/3/3. Email:yaoalong@foxmail.com
 */
public interface DSDatabase {
    M2mDataNode retrieve(String key);

    List<M2mDataNode> retrieve(Long zxid);

    Long create(Object object) throws M2mKeeperException.NodeExistsException;

    Long delete(String key);

    Long update(String key, M2mDataNode updated);

    boolean truncate(Long zxid);

    List<M2mDataNode> getCertainData(Long low, Long high);

    Long getMaxZxid(List<RangeDO> rangeDOs);

    void close();

    ProcessTxnResult processTxn(M2mTxnHeader header, M2mRecord m2mRecord);

}
