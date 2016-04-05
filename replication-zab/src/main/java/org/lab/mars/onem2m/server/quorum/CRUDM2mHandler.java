package org.lab.mars.onem2m.server.quorum;

import org.lab.mars.onem2m.proto.M2mPacket;

public class CRUDM2mHandler extends M2mHandler {

    @Override
    public M2mHandlerResult recv(M2mPacket m2mPacket) {
        M2mHandlerResult m2mHandlerResult = new M2mHandlerResult(true,
                m2mPacket);
        return m2mHandlerResult;
    }

}
