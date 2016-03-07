package org.lab.mars.onem2m.server.quorum;

import org.lab.mars.onem2m.proto.M2mPacket;

public abstract class M2mHandler {

    public abstract M2mHandlerResult recv(M2mPacket m2mPacket);

}
