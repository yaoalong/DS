package lab.mars.ds.zab;

import lab.mars.ds.loadbalance.LoadBalanceException;

import org.junit.Test;
import org.lab.mars.onem2m.server.quorum.QuorumPeerConfig;
import org.lab.mars.onem2m.server.quorum.QuorumPeerConfig.ConfigException;

public class QuorumPeerTest {

    @Test
    public void test() throws LoadBalanceException {
        QuorumPeerConfig config = new QuorumPeerConfig();
        try {
            config.parse("zoo2.cfg");
        } catch (ConfigException e) {
            e.printStackTrace();
        }

    }

}
