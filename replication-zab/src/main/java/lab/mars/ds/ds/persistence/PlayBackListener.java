package lab.mars.ds.ds.persistence;

import org.lab.mars.onem2m.jute.M2mRecord;
import org.lab.mars.onem2m.txn.M2mTxnHeader;

/**
 * Author:yaoalong.
 * Date:2016/3/17.
 * Email:yaoalong@foxmail.com
 */
public interface PlayBackListener {

    void onTxnLoaded(M2mTxnHeader hdr, M2mRecord rec);
}
