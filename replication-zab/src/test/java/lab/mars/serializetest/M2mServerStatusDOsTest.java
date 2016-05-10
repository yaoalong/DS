package lab.mars.serializetest;

import lab.mars.ds.web.protocol.M2mServerStatusDO;
import lab.mars.ds.web.protocol.M2mServerStatusDOs;
import org.junit.Test;
import org.lab.mars.onem2m.jute.M2mBinaryInputArchive;
import org.lab.mars.onem2m.jute.M2mBinaryOutputArchive;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Author:yaoalong.
 * Date:2016/5/10.
 * Email:yaoalong@foxmail.com
 */
public class M2mServerStatusDOsTest {
    static final ByteArrayOutputStream baos;
    static final M2mBinaryOutputArchive boa;
    static {
        baos = new ByteArrayOutputStream();
        boa = M2mBinaryOutputArchive.getArchive(baos);
    }
    @Test
    public void testSerialize() throws IOException {
        M2mServerStatusDOs m2mServerStatusDOs=new M2mServerStatusDOs();
        List<M2mServerStatusDO> statusDOs = new ArrayList<>();
        for(int i=0;i<5;i++){
            M2mServerStatusDO m2mServerStatusDO=new M2mServerStatusDO();
            m2mServerStatusDO.setIp("192.168.10.12"+i);
            if(i%2==0){
                m2mServerStatusDO.setStatus(0);
            }
            else{
                m2mServerStatusDO.setStatus(1);
            }
            statusDOs.add(m2mServerStatusDO);
        }
        m2mServerStatusDOs.setM2mServerStatusDOs(statusDOs);
        m2mServerStatusDOs.serialize(boa, "long");
        byte[] bytes = baos.toByteArray();
        ByteArrayInputStream byteArrayInputStream=new ByteArrayInputStream(bytes);
        M2mBinaryInputArchive binaryInputArchive=M2mBinaryInputArchive.getArchive(byteArrayInputStream);
        M2mServerStatusDOs m2mServerStatusDOs2=new M2mServerStatusDOs();
        m2mServerStatusDOs2.deserialize(binaryInputArchive,"allen");
    }
}
