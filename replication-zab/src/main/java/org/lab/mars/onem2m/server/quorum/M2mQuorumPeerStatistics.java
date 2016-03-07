package org.lab.mars.onem2m.server.quorum;

import java.util.concurrent.ConcurrentHashMap;

public class M2mQuorumPeerStatistics {

    public static volatile ConcurrentHashMap<String, M2mQuorumPeer> quorums = new ConcurrentHashMap<String, M2mQuorumPeer>();

}
