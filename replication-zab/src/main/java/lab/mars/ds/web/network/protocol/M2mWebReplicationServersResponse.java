package lab.mars.ds.web.network.protocol;

import org.lab.mars.onem2m.jute.M2mInputArchive;
import org.lab.mars.onem2m.jute.M2mOutputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Author:yaoalong.
 * Date:2016/4/10.
 * Email:yaoalong@foxmail.com
 */
public class M2mWebReplicationServersResponse implements M2mRecord {
    private static final long serialVersionUID = 3529521480116570336L;
    private List<M2mServerStatusDO> replicationServers = new ArrayList<>();

    public M2mWebReplicationServersResponse(List<M2mServerStatusDO> replicationServers) {
        this.replicationServers = replicationServers;
    }

    @Override
    public void serialize(M2mOutputArchive archive, String tag) throws IOException {

    }

    @Override
    public void deserialize(M2mInputArchive archive, String tag) throws IOException {

    }

    public List<M2mServerStatusDO> getReplicationServers() {
        return replicationServers;
    }
}
