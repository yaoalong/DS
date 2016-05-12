package org.lab.mars.onem2m.txn;

import org.lab.mars.onem2m.jute.*;

public class M2mTxnHeader implements M2mRecord {

    /**
     *
     */
    private static final long serialVersionUID = 3788714406614518665L;
    private long zxid;
    private long time;
    private int type;

    public M2mTxnHeader() {
    }

    public M2mTxnHeader(long zxid, long time, int type) {
        this.zxid = zxid;
        this.time = time;
        this.type = type;
    }

    public static String signature() {
        return "LTxnHeader(lilli)";
    }

    public long getZxid() {
        return zxid;
    }

    public void setZxid(long m_) {
        zxid = m_;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long m_) {
        time = m_;
    }

    public int getType() {
        return type;
    }

    public void setType(int m_) {
        type = m_;
    }

    public void serialize(M2mOutputArchive a_, String tag)
            throws java.io.IOException {
        a_.startRecord(this, tag);
        a_.writeLong(zxid, "zxid");
        a_.writeLong(time, "time");
        a_.writeInt(type, "type");
        a_.endRecord(this, tag);
    }

    public void deserialize(M2mInputArchive a_, String tag)
            throws java.io.IOException {
        a_.startRecord(tag);
        zxid = a_.readLong("zxid");
        time = a_.readLong("time");
        type = a_.readInt("type");
        a_.endRecord(tag);
    }

    public String toString() {
        try {
            java.io.ByteArrayOutputStream s = new java.io.ByteArrayOutputStream();
            M2mCsvOutputArchive a_ = new M2mCsvOutputArchive(s);
            a_.startRecord(this, "");
            a_.writeLong(zxid, "zxid");
            a_.writeLong(time, "time");
            a_.writeInt(type, "type");
            a_.endRecord(this, "");
            return new String(s.toByteArray(), "UTF-8");
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return "ERROR";
    }

    public void write(java.io.DataOutput out) throws java.io.IOException {
        M2mBinaryOutputArchive archive = new M2mBinaryOutputArchive(out);
        serialize(archive, "");
    }

    public void readFields(java.io.DataInput in) throws java.io.IOException {
        M2mBinaryInputArchive archive = new M2mBinaryInputArchive(in);
        deserialize(archive, "");
    }

    public int hashCode() {
        int result = 17;
        return result;
    }

}
