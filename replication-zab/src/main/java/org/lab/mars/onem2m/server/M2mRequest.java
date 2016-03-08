package org.lab.mars.onem2m.server;

import io.netty.channel.Channel;

import java.nio.ByteBuffer;

import org.lab.mars.onem2m.KeeperException;
import org.lab.mars.onem2m.ZooDefs.OpCode;
import org.lab.mars.onem2m.jute.M2mRecord;
import org.lab.mars.onem2m.txn.M2mTxnHeader;

/**
 * This is the structure that represents a request moving through a chain of
 * RequestProcessors. There are various pieces of information that is tacked
 * onto the request as it is processed.
 */
public class M2mRequest {
    public final static M2mRequest requestOfDeath = new M2mRequest(null, 0, 0,
            null);

    /**
     * @param cnxn
     * @param sessionId
     * @param xid
     * @param type
     * @param bb
     */
    public M2mRequest(Channel channel, int xid, int type, ByteBuffer bb) {
        this.channel = channel;
        this.cxid = xid;
        this.type = type;
        this.request = bb;
    }

    public final int cxid;

    public final int type;

    public Channel channel;

    public M2mTxnHeader m2mTxnHeader;

    public M2mRecord txn;

    public long zxid = -1;

    public final ByteBuffer request;

    public final long createTime = System.currentTimeMillis();

    private KeeperException e;

    /**
     * is the packet type a valid packet in zookeeper
     * 
     * @param type
     *            the type of the packet
     * @return true if a valid packet, false if not
     */
    static boolean isValid(int type) {
        // make sure this is always synchronized with Zoodefs!!
        switch (type) {
        case OpCode.notification:
            return false;
        case OpCode.create:
        case OpCode.delete:

        case OpCode.getData:
        case OpCode.setData:
        case OpCode.sync:
            return true;
        default:
            return false;
        }
    }

    static boolean isQuorum(int type) {
        switch (type) {
        case OpCode.getData:
            return false;
        case OpCode.error:
        case OpCode.create:
        case OpCode.delete:
        case OpCode.setData:
            return true;
        default:
            return false;
        }
    }

    static String op2String(int op) {
        switch (op) {
        case OpCode.notification:
            return "notification";
        case OpCode.create:
            return "create";
        case OpCode.delete:
            return "delete";
        case OpCode.getData:
            return "getData";
        case OpCode.setData:
            return "setData";
        case OpCode.sync:
            return "sync:";
        case OpCode.error:
            return "error";
        default:
            return "unknown " + op;
        }
    }

    // @Override
    // public String toString() {
    // StringBuilder sb = new StringBuilder();
    // sb.append("sessionid:0x").append(Long.toHexString(sessionId))
    // .append(" type:").append(op2String(type))
    // .append(" cxid:0x").append(Long.toHexString(cxid))
    // .append(" zxid:0x").append(Long.toHexString(hdr == null ?
    // -2 : hdr.getZxid()))
    // .append(" txntype:").append(hdr == null ?
    // "unknown" : "" + hdr.getType());
    //
    // // best effort to print the path assoc with this request
    // String path = "n/a";
    // if (type != OpCode.createSession
    // && type != OpCode.setWatches
    // && type != OpCode.closeSession
    // && request != null
    // && request.remaining() >= 4)
    // {
    // try {
    // // make sure we don't mess with request itself
    // ByteBuffer rbuf = request.asReadOnlyBuffer();
    // rbuf.clear();
    // int pathLen = rbuf.getInt();
    // // sanity check
    // if (pathLen >= 0
    // && pathLen < 4096
    // && rbuf.remaining() >= pathLen)
    // {
    // byte b[] = new byte[pathLen];
    // rbuf.get(b);
    // path = new String(b);
    // }
    // } catch (Exception e) {
    // // ignore - can't find the path, will output "n/a" instead
    // }
    // }
    // sb.append(" reqpath:").append(path);
    //
    // return sb.toString();
    // }

    public void setException(KeeperException e) {
        this.e = e;
    }

    public KeeperException getException() {
        return e;
    }

    public ByteBuffer getRequest() {
        return request;
    }

}
