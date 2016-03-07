package lab.mars.ds.zab;

import org.junit.Test;
import org.lab.mars.onem2m.server.quorum.QuorumPeerConfig;
import org.lab.mars.onem2m.server.quorum.QuorumPeerConfig.ConfigException;

public class QuorumPeerTest {

    @Test
    public void test() {
        QuorumPeerConfig config = new QuorumPeerConfig();
        try {
            config.parse("zoo2.cfg");
        } catch (ConfigException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
