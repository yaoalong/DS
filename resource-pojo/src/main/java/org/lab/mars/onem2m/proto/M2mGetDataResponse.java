package org.lab.mars.onem2m.proto;

import org.lab.mars.onem2m.jute.*;

import java.io.IOException;

/**
 * @author yaoalong
 * @Date 2016年1月26日
 * @Email yaoalong@foxmail.com Get data response
 */
public class M2mGetDataResponse implements M2mRecord {

    /**
     *
     */
    private static final long serialVersionUID = 9126015763824123503L;
    private byte[] data;

    public M2mGetDataResponse() {
    }

    public M2mGetDataResponse(byte[] data) {
        this.data = data;
    }

    public static String signature() {
        return "LGetDataResponse(BLStat(lllliiiliil))";
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] m_) {
        data = m_;
    }

    public void serialize(M2mOutputArchive a_, String tag)
            throws IOException {
        a_.startRecord(this, tag);
        a_.writeBuffer(data, "data");

        a_.endRecord(this, tag);
    }

    @Override
    public void deserialize(M2mInputArchive a_, String tag) throws IOException {
        a_.startRecord(tag);
        data = a_.readBuffer("data");
        a_.endRecord(tag);
    }

    public String toString() {
        try {
            java.io.ByteArrayOutputStream s = new java.io.ByteArrayOutputStream();
            M2mCsvOutputArchive a_ = new M2mCsvOutputArchive(s);
            a_.startRecord(this, "");
            a_.writeBuffer(data, "data");

            a_.endRecord(this, "");
            return new String(s.toByteArray(), "UTF-8");
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return "ERROR";
    }

    public void write(java.io.DataOutput out) throws IOException {
        M2mBinaryOutputArchive archive = new M2mBinaryOutputArchive(out);
        serialize(archive, "");
    }

    public void readFields(java.io.DataInput in) throws IOException {
        M2mBinaryInputArchive archive = new M2mBinaryInputArchive(in);
        deserialize(archive, "");
    }




}
