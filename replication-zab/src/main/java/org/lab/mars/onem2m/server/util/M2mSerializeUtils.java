package org.lab.mars.onem2m.server.util;

import org.lab.mars.onem2m.ZooDefs.OpCode;
import org.lab.mars.onem2m.jute.M2mBinaryInputArchive;
import org.lab.mars.onem2m.jute.M2mInputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;
import org.lab.mars.onem2m.txn.M2mCreateTxn;
import org.lab.mars.onem2m.txn.M2mDeleteTxn;
import org.lab.mars.onem2m.txn.M2mSetDataTxn;
import org.lab.mars.onem2m.txn.M2mTxnHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;

public class M2mSerializeUtils {
    private static final Logger LOG = LoggerFactory
            .getLogger(M2mSerializeUtils.class);

    public static M2mRecord deserializeTxn(byte txnBytes[], M2mTxnHeader hdr)
            throws IOException {
        final ByteArrayInputStream bais = new ByteArrayInputStream(txnBytes);
        M2mInputArchive ia = M2mBinaryInputArchive.getArchive(bais);

        hdr.deserialize(ia, "hdr");
        bais.mark(bais.available());
        M2mRecord txn ;
        switch (hdr.getType()) {
            case OpCode.create:
                txn = new M2mCreateTxn();
                break;
            case OpCode.delete:
                txn = new M2mDeleteTxn();
                break;
            case OpCode.setData:
                txn = new M2mSetDataTxn();
                break;
            default:
                throw new IOException("Unsupported Txn with type=%d"
                        + hdr.getType());
        }
        if (txn != null) {
            try {
                txn.deserialize(ia, "txn");
            } catch (EOFException e) {
                throw e;
            }
        }
        return txn;
    }


}
