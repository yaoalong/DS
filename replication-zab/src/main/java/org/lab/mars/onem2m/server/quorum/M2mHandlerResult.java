package org.lab.mars.onem2m.server.quorum;

import org.lab.mars.onem2m.proto.M2mPacket;

public class M2mHandlerResult {
    private boolean flag = false;

    private M2mPacket m2mPacket;

    public M2mHandlerResult(boolean flag, M2mPacket m2mPacket) {
        this.flag = flag;
        this.m2mPacket = m2mPacket;

    }

    public boolean isFlag() {
        return flag;
    }

    public M2mPacket getM2mPacket() {
        return m2mPacket;
    }
}
