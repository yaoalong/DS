package org.lab.mars.ds.server;

import org.lab.mars.onem2m.jute.M2mInputArchive;
import org.lab.mars.onem2m.jute.M2mOutputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;

import java.io.IOException;

/**
 * Author:yaoalong.
 * Date:2016/3/5.
 * Email:yaoalong@foxmail.com
 */
public class M2mDataNode implements M2mRecord {

    /**
     *
     */
    private static final long serialVersionUID = 3291328270207258803L;
    public int label;
    public long zxid;
    public String id;
    public byte[] data;

    public long value;

    public int flag;

    public Integer getLabel() {
        return label;
    }

    public void setLabel(Integer label) {
        this.label = label;
    }

    public Long getZxid() {
        return zxid;
    }

    public void setZxid(Long zxid) {
        this.zxid = zxid;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void serialize(M2mOutputArchive archive, String tag)
            throws IOException {
        archive.startRecord(this, tag);
        archive.writeInt(label, "label");
        archive.writeLong(zxid, "zxid");
        archive.writeString(id, "id");
        archive.writeBuffer(data, "data");
        archive.writeLong(value, "value");
        archive.endRecord(this, tag);

    }

    @Override
    public void deserialize(M2mInputArchive archive, String tag)
            throws IOException {
        archive.startRecord("node");
        label = archive.readInt("label");
        zxid = archive.readLong("zxid");
        id = archive.readString("id");
        data = archive.readBuffer("data");
        value = archive.readLong("value");
        archive.endRecord(tag);
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
