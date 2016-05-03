package lab.mars.ds.web.network.protocol;

import io.netty.channel.ChannelHandlerContext;

import java.util.List;

/**
 * Author:yaoalong.
 * Date:2016/5/3.
 * Email:yaoalong@foxmail.com
 */
public class ServerLoadAndCtx {
    private ChannelHandlerContext ctx;

    private List<M2mServerLoadDO> m2mServerLoadDOs;

    public ServerLoadAndCtx(ChannelHandlerContext ctx, List<M2mServerLoadDO> m2mServerLoadDOs) {
        this.ctx = ctx;
        this.m2mServerLoadDOs = m2mServerLoadDOs;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public void setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public List<M2mServerLoadDO> getM2mServerLoadDOs() {
        return m2mServerLoadDOs;
    }

    public void setM2mServerLoadDOs(List<M2mServerLoadDO> m2mServerLoadDOs) {
        this.m2mServerLoadDOs = m2mServerLoadDOs;
    }
}
