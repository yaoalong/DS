package test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import lab.mars.ds.loadbalance.RangeDO;
import lab.mars.ds.persistence.DSDatabaseImpl;

import org.junit.Test;
import org.lab.mars.ds.server.M2mDataNode;
import org.lab.mars.onem2m.M2mKeeperException;
import org.lab.mars.onem2m.ZooDefs.OpCode;
import org.lab.mars.onem2m.jute.M2mBinaryOutputArchive;
import org.lab.mars.onem2m.txn.M2mSetDataTxn;
import org.lab.mars.onem2m.txn.M2mTxnHeader;

public class M2mDataBaseTest {
    DSDatabaseImpl m2mDataBase = new DSDatabaseImpl(false, "tests", "onem2m1",
            "192.168.10.124");

    @Test
    public void test() throws M2mKeeperException {
        m2mDataBase.truncate((long) 4);
    }

    @Test
    public void testRetrieve() throws M2mKeeperException {
        List<RangeDO> rangeDOs = new ArrayList<RangeDO>();
        rangeDOs.add(new RangeDO(0L, 2429425133L));
        rangeDOs.add(new RangeDO(2429425133L, 2611217912L));
        rangeDOs.add(new RangeDO(3582305062L, 3689081781L));
        rangeDOs.add(new RangeDO(3848679456L, 9223372036854775807L));
        m2mDataBase.getMaxZxid(rangeDOs);
        M2mDataNode m2mDataNode = m2mDataBase.retrieve("223232");
        TraversalAllFields.getObjAttr(m2mDataNode);
    }

    @Test
    public void testDelete() throws M2mKeeperException {
        m2mDataBase.delete("3333433");
    }

    @Test
    public void testRetrieve1() throws M2mKeeperException {

        List<M2mDataNode> m2mDataNodes = m2mDataBase.retrieve(634L);
        m2mDataNodes.forEach(m2mDataNode -> {
            TraversalAllFields.getObjAttr(m2mDataNode);
        });

    }

    @Test
    public void testUpdate() throws M2mKeeperException {
        List<RangeDO> rangeDOs = new ArrayList<RangeDO>();
        rangeDOs.add(new RangeDO(0L, 2429425133L));
        rangeDOs.add(new RangeDO(2429425133L, 2611217912L));
        rangeDOs.add(new RangeDO(3582305062L, 3689081781L));
        rangeDOs.add(new RangeDO(3848679456L, 9223372036854775807L));
        m2mDataBase.getMaxZxid(rangeDOs);
        M2mDataNode m2mDataNode = new M2mDataNode();
        m2mDataNode.setId("223232");
        m2mDataNode.setData("666666".getBytes());
        m2mDataBase.update("223232", m2mDataNode);
    }

    @Test
    public void testCreate() throws M2mKeeperException {
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            M2mDataNode m2mDataNode = new M2mDataNode();

            m2mDataNode.setZxid(Long.valueOf(1402500 + "") + random.nextLong());
            m2mDataNode.setData("2+".getBytes());
            m2mDataNode.setId((99333130 + i) + "");
            m2mDataNode.setLabel(0);
            m2mDataNode.setValue(11L + i);
            m2mDataBase.create(m2mDataNode);
        }

    }

    @Test
    public void testGetCertainValue() {
        List<M2mDataNode> m2mDataNodes = m2mDataBase.getCertainData(12L, 15L);
        for (M2mDataNode m2mDataNode : m2mDataNodes) {
            TraversalAllFields.getObjAttr(m2mDataNode);
        }
    }

    @Test
    public void testProcessTxn() throws IOException {
        M2mTxnHeader m2mTxnHeader = new M2mTxnHeader();
        m2mTxnHeader.setType(OpCode.setData);
        M2mSetDataTxn m2mSetDataTxn = new M2mSetDataTxn();
        m2mSetDataTxn.setId("11111");
        M2mDataNode m2mDataNode = new M2mDataNode();
        m2mDataNode.setId(11111 + "");
        m2mDataNode.setLabel(0);
        m2mDataNode.setZxid(Long.valueOf("9999"));
        m2mDataNode.setData("343423".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        M2mBinaryOutputArchive boa = M2mBinaryOutputArchive.getArchive(baos);
        m2mDataNode.serialize(boa, "m2mDataNode");
        m2mSetDataTxn.setData(baos.toByteArray());
        m2mDataBase.processTxn(m2mTxnHeader, m2mSetDataTxn);

    }

    @Test
    public void testGetMaxZxid() {
        // System.out.println(m2mDataBase.getMaxZxid(3689081781L,
        // 9223372036854775807L, 0L, 0L));
    }

    @Test
    public void testMod() {
        System.out.println((-1 + 6) % 6);

    }
}
