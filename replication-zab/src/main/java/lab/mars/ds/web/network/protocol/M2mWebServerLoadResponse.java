package lab.mars.ds.web.network.protocol;

import org.lab.mars.onem2m.jute.M2mInputArchive;
import org.lab.mars.onem2m.jute.M2mOutputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;

import java.io.IOException;
import java.util.List;

/**
 * Author:yaoalong. Date:2016/4/10. Email:yaoalong@foxmail.com
 */
public class M2mWebServerLoadResponse implements M2mRecord {
    /**
     * 
     */
    private static final long serialVersionUID = -4577604667510258541L;

    public M2mWebServerLoadResponse(List<M2mServerLoadDO> m2mServerLoadDOs){
        this.m2mServerLoadDOs=m2mServerLoadDOs;
    }
    private List<M2mServerLoadDO> m2mServerLoadDOs;
    @Override
    public void serialize(M2mOutputArchive archive, String tag)
            throws IOException {

    }

    @Override
    public void deserialize(M2mInputArchive archive, String tag)
            throws IOException {

    }

    public List<M2mServerLoadDO> getM2mServerLoadDOs() {
        return m2mServerLoadDOs;
    }
}
