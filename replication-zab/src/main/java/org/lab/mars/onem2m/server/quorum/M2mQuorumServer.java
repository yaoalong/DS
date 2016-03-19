package org.lab.mars.onem2m.server.quorum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.lab.mars.onem2m.server.quorum.M2mQuorumPeer.QuorumServer;

public class M2mQuorumServer {
    private List<HashMap<Long, QuorumServer>> quorumsList;

    private List<String> servers = new ArrayList<>();

    public List<HashMap<Long, QuorumServer>> getQuorumsList() {
        return quorumsList;
    }

    public void setQuorumsList(List<HashMap<Long, QuorumServer>> quorumsList) {
        this.quorumsList = quorumsList;
    }

    public List<String> getServers() {
        return servers;
    }

    public void setServers(List<String> servers) {
        this.servers = servers;
    }

}
