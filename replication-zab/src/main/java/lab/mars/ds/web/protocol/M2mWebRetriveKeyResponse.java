package lab.mars.ds.web.protocol;

import org.lab.mars.onem2m.jute.*;

import java.util.ArrayList;
import java.util.List;


public class M2mWebRetriveKeyResponse implements M2mRecord {
    /**
     *
     */
    private static final long serialVersionUID = 3367783772293345229L;
    private List<String> servers;

    public M2mWebRetriveKeyResponse(List<String> servers) {
        this.servers = servers;
    }

    public static String signature() {
        return "LGetDataResponse(BLStat(lllliiiliil))";
    }

    public List<String> getServers() {
        return servers;
    }

    public void setServers(List<String> servers) {
        this.servers = servers;
    }

    public void serialize(M2mOutputArchive a_, String tag)
            throws java.io.IOException {
        a_.startRecord(this, tag);
        a_.writeInt(servers.size(), "size");
        for (String server : servers) {
            a_.writeString(server, "server");
        }
        a_.endRecord(this, tag);
    }

    public void deserialize(M2mInputArchive a_, String tag)
            throws java.io.IOException {
        a_.startRecord(tag);
        int size = a_.readInt("size");
        List<String> serverStrings = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String server = a_.readString("server");
            serverStrings.add(server);
        }
        servers = serverStrings;
        a_.endRecord(tag);
    }

    public String toString() {
        try {
            java.io.ByteArrayOutputStream s = new java.io.ByteArrayOutputStream();
            M2mCsvOutputArchive a_ = new M2mCsvOutputArchive(s);
            a_.startRecord(this, "");
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
