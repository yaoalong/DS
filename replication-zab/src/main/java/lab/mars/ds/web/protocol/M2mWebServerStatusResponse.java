package lab.mars.ds.web.protocol;

import org.lab.mars.onem2m.jute.*;

public class M2mWebServerStatusResponse implements M2mRecord {

    /**
     *
     */
    private static final long serialVersionUID = -1163005631887805265L;
    private byte[] data;

    public M2mWebServerStatusResponse() {
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
            throws java.io.IOException {
        a_.startRecord(this, tag);
        a_.writeBuffer(data, "data");
        a_.endRecord(this, tag);
    }

    public void deserialize(M2mInputArchive a_, String tag)
            throws java.io.IOException {
        a_.startRecord(tag);
        data = a_.readBuffer("data");
        a_.endRecord(tag);
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