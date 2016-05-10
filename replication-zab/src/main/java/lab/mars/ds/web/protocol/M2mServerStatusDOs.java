package lab.mars.ds.web.protocol;

import org.lab.mars.onem2m.jute.M2mInputArchive;
import org.lab.mars.onem2m.jute.M2mOutputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class M2mServerStatusDOs implements M2mRecord {

    /**
     * 
     */
    private static final long serialVersionUID = -6064957306675458810L;
    private List<M2mServerStatusDO> m2mServerStatusDOs;

    public List<M2mServerStatusDO> getM2mServerStatusDOs() {
        return m2mServerStatusDOs;
    }

    public void setM2mServerStatusDOs(List<M2mServerStatusDO> m2mServerStatusDOs) {
        this.m2mServerStatusDOs = m2mServerStatusDOs;
    }

    @Override
    public void serialize(M2mOutputArchive archive, String tag)
            throws IOException {
        archive.startRecord(this, tag);
        archive.writeInt(m2mServerStatusDOs.size(), "size");
        for (M2mServerStatusDO m2mServerStatusDO : m2mServerStatusDOs) {
            archive.writeLong(m2mServerStatusDO.getId(), "id");
            archive.writeString(m2mServerStatusDO.getIp(), "ip");
            System.out.println("status"+m2mServerStatusDO.getStatus());
            archive.writeInt(m2mServerStatusDO.getStatus(), "status");
        }
        archive.endRecord(this, tag);

    }

    @Override
    public void deserialize(M2mInputArchive archive, String tag)
            throws IOException {
        List<M2mServerStatusDO> m2mServerStatusDOs = new ArrayList<M2mServerStatusDO>();

        archive.startRecord(tag);
        int length = archive.readInt("length");
        for (int i = 0; i < length; i++) {
            M2mServerStatusDO m2mServerStatusDO = new M2mServerStatusDO();
            m2mServerStatusDO.setId(archive.readLong("id"));
            m2mServerStatusDO.setIp(archive.readString("ip"));
            int status=archive.readInt("status");
            System.out.println("status:"+status);
            m2mServerStatusDO.setStatus(status);
            m2mServerStatusDOs.add(m2mServerStatusDO);
        }
        archive.endRecord(tag);
        this.m2mServerStatusDOs = m2mServerStatusDOs;
    }

}
