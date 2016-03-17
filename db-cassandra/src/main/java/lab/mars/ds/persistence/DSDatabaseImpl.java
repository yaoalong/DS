package lab.mars.ds.persistence;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import lab.mars.ds.loadbalance.RangeDO;
import lab.mars.ds.loadbalance.impl.NetworkPool;
import lab.mars.ds.reflection.ResourceReflection;
import org.lab.mars.ds.server.M2mDataNode;
import org.lab.mars.ds.server.ProcessTxnResult;
import org.lab.mars.onem2m.M2mKeeperException;
import org.lab.mars.onem2m.ZooDefs;
import org.lab.mars.onem2m.jute.M2mBinaryOutputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;
import org.lab.mars.onem2m.txn.M2mCreateTxn;
import org.lab.mars.onem2m.txn.M2mDeleteTxn;
import org.lab.mars.onem2m.txn.M2mSetDataTxn;
import org.lab.mars.onem2m.txn.M2mTxnHeader;

import java.io.ByteArrayOutputStream;
import java.util.*;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;

/**
 * Author:yaoalong. Date:2016/3/3. Email:yaoalong@foxmail.com
 */
public class DSDatabaseImpl implements DSDatabase {
    static ByteArrayOutputStream baos = new ByteArrayOutputStream();
    static M2mBinaryOutputArchive boa = M2mBinaryOutputArchive.getArchive(baos);
    private String keyspace;
    private String table;
    private String node;
    private Cluster cluster;
    private Session session;
    private boolean clean = false;

    private TreeMap<Long, RangeDO> endRangeDOMap = new TreeMap<Long, RangeDO>();

    public DSDatabaseImpl() {
        this(false, "mars", "onem2m", "127.0.0.1");
    }

    public DSDatabaseImpl(boolean clean, String keyspace, String table,
                          String node) {
        this.clean = clean;
        this.keyspace = keyspace;
        this.table = table;
        this.node = node;
        connect();
    }

    private static Long getZxid(ResultSet resultSet) {
        List<Long> zxids = new ArrayList<Long>();
        for (Row row : resultSet.all()) {
            ColumnDefinitions columnDefinitions = resultSet
                    .getColumnDefinitions();
            columnDefinitions.forEach(d -> {
                String name = d.getName();
                if (name.equals("zxid")) {
                    zxids.add((Long) row.getObject(name));
                }

            });
        }
        if (zxids.size() == 0) {
            return 0L;
        } else {
            return zxids.get(zxids.size() - 1);
        }
    }

    private List<M2mDataNode> getM2mDataNodes(ResultSet resultSet) {
        List<M2mDataNode> m2mDataNodes = new ArrayList<>();
        Map<String, Object> result = new HashMap<String, Object>();
        for (Row row : resultSet.all()) {
            ColumnDefinitions columnDefinitions = resultSet
                    .getColumnDefinitions();
            columnDefinitions.forEach(d -> {
                String name = d.getName();
                Object object = row.getObject(name);
                result.put(name, object);
            });
            System.out.println("zxid:" + result.get("zxid"));
            if (judgeIsHandle((Long) result.get("zxid"))) {
                m2mDataNodes.add(ResourceReflection.deserialize(
                        M2mDataNode.class, result));
            }

            result.clear();
        }
        return m2mDataNodes;
    }

    public void connect() {
        cluster = Cluster.builder().addContactPoint(node).build();
        Metadata metadata = cluster.getMetadata();
        System.out.printf("Connected to cluster: %s\n",
                metadata.getClusterName());
        for (Host host : metadata.getAllHosts()) {
            System.out.printf("Datacenter: %s; Host: %s; Rack: %s\n",
                    host.getDatacenter(), host.getAddress(), host.getRack());
        }
        session = cluster.connect();
        if (clean) {
            session.execute("use " + keyspace + ";");
            session.execute("truncate " + table + ";");
        }
    }

    /**
     * 检索特定的key
     */
    @Override
    public M2mDataNode retrieve(String key) {
        try {
            Select.Selection selection = query().select();
            Select select = selection.from(keyspace, table);
            select.where(eq("id", key));
            select.allowFiltering();
            ResultSet resultSet = session.execute(select);
            if (resultSet == null) {
                return null;
            }
            List<M2mDataNode> m2mDataNodes = getM2mDataNodes(resultSet);
            return m2mDataNodes.get(0);

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * 插入数据
     */
    @Override
    public Long create(Object object)
            throws M2mKeeperException.NodeExistsException {

        M2mDataNode m2mDataNode = retrieve(((M2mDataNode) object).getId());
        if (m2mDataNode != null && m2mDataNode.getId() != null) {
            throw new M2mKeeperException.NodeExistsException();
        }
        Map<String, Object> map = ResourceReflection.serialize(object);
        Insert insert = query().insertInto(keyspace, table);
        map.forEach(insert::value);
        session.execute(insert);
        return 1L;
    }

    @Override
    public Long delete(String key) {
        try {
            M2mDataNode m2mDataNode = retrieve(key);
            Statement delete = query().delete().from(keyspace, table)
                    .where(eq("label", 0))
                    .and(eq("zxid", m2mDataNode.getZxid()));
            session.execute(delete);
        } catch (Exception ex) {
            ex.printStackTrace();
            return Long.valueOf(0);
        }
        return Long.valueOf(1);
    }

    @Override
    public Long update(String key, M2mDataNode updated) {
        try {
            M2mDataNode m2mDataNode = retrieve(key);
            if (m2mDataNode == null) {
                return 0L;
            }
            delete(key);
            m2mDataNode.setData(updated.getData());
            create(m2mDataNode);
        } catch (Exception ex) {
            ex.printStackTrace();
            return 0L;
        }
        return 1L;
    }

    private QueryBuilder query() {
        return new QueryBuilder(cluster);
    }

    @Override
    public void close() {

        if (session != null) {
            session.close();
        }
        if (cluster != null) {
            cluster.close();
        }
    }

    /**
     * 最终将事务请求应用到cassandra数据库上
     */
    @Override
    public ProcessTxnResult processTxn(M2mTxnHeader header, M2mRecord m2mRecord) {
        ProcessTxnResult processTxnResult = new ProcessTxnResult();
        try {
            processTxnResult.cxid = header.getCxid();
            processTxnResult.zxid = header.getZxid();
            processTxnResult.err = 0;
            switch (header.getType()) {
                case ZooDefs.OpCode.create:
                    M2mCreateTxn createTxn = (M2mCreateTxn) m2mRecord;
                    processTxnResult.path = createTxn.getPath();
                    M2mDataNode m2mDataNode = (M2mDataNode) ResourceReflection
                            .deserializeKryo(createTxn.getData());
                    m2mDataNode.setValue(NetworkPool.md5HashingAlg(m2mDataNode
                            .getId()));
                    m2mDataNode.setZxid(header.getZxid());
                    create(m2mDataNode);
                    break;
                case ZooDefs.OpCode.delete:
                    M2mDeleteTxn deleteTxn = (M2mDeleteTxn) m2mRecord;
                    processTxnResult.path = deleteTxn.getPath();
                    delete(deleteTxn.getPath());
                    break;
                case ZooDefs.OpCode.setData:
                    M2mSetDataTxn m2mSetDataTxn = (M2mSetDataTxn) m2mRecord;
                    processTxnResult.path = m2mSetDataTxn.getPath();
                    M2mDataNode object = (M2mDataNode) ResourceReflection
                            .deserializeKryo(m2mSetDataTxn.getData());
                    update(m2mSetDataTxn.getPath(), object);
                    break;
            }
        } catch (M2mKeeperException e) {
            processTxnResult.err = e.getCode().intValue();
        }

        return processTxnResult;
    }

    @Override
    public boolean truncate(Long zxid) {
        try {

            List<M2mDataNode> m2mDataNodes = retrieve(zxid);
            for (M2mDataNode m2mDataNode : m2mDataNodes) {
                delete(m2mDataNode.getId());
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 检索比特定zxid大的值
     *
     * @param zxid
     * @return
     */
    @Override
    public List<M2mDataNode> retrieve(Long zxid) {
        try {
            Select.Selection selection = query().select();
            Select select = selection.from(keyspace, table);
            select.where(gt("zxid", zxid));
            select.allowFiltering();
            ResultSet resultSet = session.execute(select);
            if (resultSet == null) {
                return null;
            }

            return getM2mDataNodes(resultSet);

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public List<M2mDataNode> getCertainData(Long low, Long high) {
        try {
            Select.Selection selection = query().select();
            Select select = selection.from(keyspace, table);
            select.where(eq("flag", 0)).and(gt("value", low))
                    .and(lt("value", high));
            select.allowFiltering();
            ResultSet resultSet = session.execute(select);
            if (resultSet == null) {
                return null;
            }
            return getM2mDataNodes(resultSet);

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private Long getMaxByCertainRange(RangeDO rangeDO) {
        long result;
        Select.Selection selection = query().select();
        Select select = selection.from(keyspace, table);
        select.where(eq("flag", 0)).and(gt("value", rangeDO.getStart()))
                .and(lt("value", rangeDO.getEnd()));
        select.allowFiltering();
        ResultSet resultSet = session.execute(select);
        result = getZxid(resultSet);
        return result;
    }

    private boolean judgeIsHandle(long zxid) {
        SortedMap<Long, RangeDO> tmap = this.endRangeDOMap.tailMap(zxid);

        Long position = (tmap.isEmpty()) ? this.endRangeDOMap.firstKey() : tmap
                .firstKey();
        RangeDO rangeDO = endRangeDOMap.get(position);
        if (rangeDO != null && rangeDO.getStart() < zxid) {
            return true;
        }
        return false;
    }

    @Override
    public Long getMaxZxid(List<RangeDO> rangeDOs) {
        long result = 0;
        for (RangeDO rangeDO : rangeDOs) {
            endRangeDOMap.put(rangeDO.getEnd(), rangeDO);
            long temp = getMaxByCertainRange(rangeDO);
            if (temp > result) {
                result = temp;
            }

        }
        return result;

    }
}
