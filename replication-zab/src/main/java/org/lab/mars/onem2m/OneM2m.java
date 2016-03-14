package org.lab.mars.onem2m;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;

import lab.mars.ds.network.TcpClient;
import lab.mars.ds.reflection.ResourceReflection;

import org.lab.mars.ds.server.M2mDataNode;
import org.lab.mars.onem2m.jute.M2mBinaryOutputArchive;
import org.lab.mars.onem2m.proto.M2mCreateRequest;
import org.lab.mars.onem2m.proto.M2mCreateResponse;
import org.lab.mars.onem2m.proto.M2mDeleteRequest;
import org.lab.mars.onem2m.proto.M2mGetDataRequest;
import org.lab.mars.onem2m.proto.M2mGetDataResponse;
import org.lab.mars.onem2m.proto.M2mPacket;
import org.lab.mars.onem2m.proto.M2mReplyHeader;
import org.lab.mars.onem2m.proto.M2mRequestHeader;
import org.lab.mars.onem2m.proto.M2mSetDataRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yaoalong
 * @Date 2016年2月20日
 * @Email yaoalong@foxmail.com
 *        <p>
 *        客户端入口
 */
public class OneM2m {
    private static final Logger LOG = LoggerFactory.getLogger(OneM2m.class);

    private TcpClient tcpClient;

    public OneM2m(String host, Integer port) {
        tcpClient = new TcpClient(new LinkedList<M2mPacket>());

        System.out.println("host:" + host);
        System.out.println("port:" + port);

        tcpClient.connectionOne(host, port);
    }

    public String create(final String path, byte[] data) throws IOException {
        M2mRequestHeader m2mRequestHeader = new M2mRequestHeader();
        m2mRequestHeader.setType(ZooDefs.OpCode.create);
        m2mRequestHeader.setKey(path);
        M2mCreateRequest m2mCreateRequest = new M2mCreateRequest();
        M2mCreateResponse m2mCreateResponse = new M2mCreateResponse();
        M2mReplyHeader m2mReplyHeader = new M2mReplyHeader();
        m2mCreateRequest.setKey(path);
        M2mDataNode m2mDataNode = new M2mDataNode();
        m2mDataNode.setId(path);
        m2mDataNode.setData(data);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        M2mBinaryOutputArchive boa = M2mBinaryOutputArchive.getArchive(baos);
        m2mDataNode.serialize(boa, "m2mData");
        byte[] bytes = baos.toByteArray();
        m2mCreateRequest.setData(bytes);
        M2mPacket m2mPacket = new M2mPacket(m2mRequestHeader, m2mReplyHeader,
                m2mCreateRequest, m2mCreateResponse);
        tcpClient.write(m2mPacket);
        int i = m2mPacket.getM2mReplyHeader().getErr();
        System.out.println(i);
        return "";
    }

    public void delete(final String path) {
        M2mRequestHeader m2mRequestHeader = new M2mRequestHeader();
        m2mRequestHeader.setType(ZooDefs.OpCode.delete);
        m2mRequestHeader.setKey(path);
        M2mDeleteRequest m2mDeleteRequest = new M2mDeleteRequest(path);
        M2mReplyHeader m2mReplyHeader = new M2mReplyHeader();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        M2mBinaryOutputArchive.getArchive(baos);
        M2mPacket m2mPacket = new M2mPacket(m2mRequestHeader, m2mReplyHeader,
                m2mDeleteRequest, new M2mCreateResponse());
        tcpClient.write(m2mPacket);
    }

    public void setData(final String path, byte[] data) {
        M2mRequestHeader m2mRequestHeader = new M2mRequestHeader();
        m2mRequestHeader.setType(ZooDefs.OpCode.setData);
        m2mRequestHeader.setKey(path);
        M2mSetDataRequest m2mSetDataRequest = new M2mSetDataRequest();
        M2mDataNode m2mDataNode = new M2mDataNode();
        m2mDataNode.setId(path);
        m2mDataNode.setData(data);
        M2mReplyHeader m2mReplyHeader = new M2mReplyHeader();
        m2mSetDataRequest
                .setData(ResourceReflection.serializeKryo(m2mDataNode));
        m2mSetDataRequest.setKey(path);
        M2mPacket m2mPacket = new M2mPacket(m2mRequestHeader, m2mReplyHeader,
                m2mSetDataRequest, new M2mCreateResponse());
        tcpClient.write(m2mPacket);

    }

    public String getData(final String path) {
        M2mRequestHeader m2mRequestHeader = new M2mRequestHeader();
        m2mRequestHeader.setType(ZooDefs.OpCode.getData);
        m2mRequestHeader.setKey(path);
        M2mGetDataRequest m2mGetDataRequest = new M2mGetDataRequest();
        M2mReplyHeader m2mReplyHeader = new M2mReplyHeader();
        m2mGetDataRequest.setPath(path);
        M2mGetDataResponse m2mGetDataResponse = new M2mGetDataResponse();
        M2mPacket m2mPacket = new M2mPacket(m2mRequestHeader, m2mReplyHeader,
                m2mGetDataRequest, m2mGetDataResponse);
        tcpClient.write(m2mPacket);
        M2mDataNode m2mDataNode = (M2mDataNode) ResourceReflection
                .deserializeKryo(((M2mGetDataResponse) m2mPacket.getResponse())
                        .getData());
        return new String(m2mDataNode.getData());
    }

    public void send(M2mPacket m2mPacket) {
        tcpClient.write(m2mPacket);
    }

    public static void main(String args[]) throws IOException {
        OneM2m oneM2m = new OneM2m("192.168.10.131", 2184);
        String key = "ddd32fds";
        oneM2m.create(key, "111".getBytes());
        oneM2m.setData(key, "5555".getBytes());
        System.out.println(oneM2m.getData(key));

    }

}
