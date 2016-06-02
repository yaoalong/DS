package lab.mars.ds.web.protocol;

import org.lab.mars.onem2m.jute.M2mBinaryOutputArchive;
import org.lab.mars.onem2m.jute.M2mInputArchive;
import org.lab.mars.onem2m.jute.M2mOutputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;

import java.util.List;

public class M2mWebServerStatusResponse implements M2mRecord {

    /**
     *
     */
    private static final long serialVersionUID = -1163005631887805265L;

    private List<M2mServerStatusDO> m2mServerStatusDOList;

    public M2mWebServerStatusResponse() {
    }


    public void serialize(M2mOutputArchive a_, String tag)
            throws java.io.IOException {

    }

    public void deserialize(M2mInputArchive a_, String tag)
            throws java.io.IOException {

    }


    public void write(java.io.DataOutput out) throws java.io.IOException {
        M2mBinaryOutputArchive archive = new M2mBinaryOutputArchive(out);
        serialize(archive, "");
    }

    public int hashCode() {
        int result = 17;

        return result;
    }

    public List<M2mServerStatusDO> getM2mServerStatusDOList() {
        return m2mServerStatusDOList;
    }

    public void setM2mServerStatusDOList(List<M2mServerStatusDO> m2mServerStatusDOList) {
        this.m2mServerStatusDOList = m2mServerStatusDOList;
    }
}
