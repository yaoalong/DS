package org.lab.mars.onem2m.server.quorum.flexible;

import java.util.HashSet;

public interface M2mQuorumVerifier {
    boolean containsQuorum(HashSet<Long> set);
}
