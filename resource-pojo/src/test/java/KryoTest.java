import lab.mars.ds.reflection.ResourceReflection;
import org.junit.Test;
import org.lab.mars.ds.server.M2mDataNode;

import java.util.Map;

/**
 * Author:yaoalong.
 * Date:2016/3/14.
 * Email:yaoalong@foxmail.com
 */
public class KryoTest {
    @Test
    public void test() {
        M2mDataNode m2mDataNode = new M2mDataNode();
        m2mDataNode.setData("yaoalong".getBytes());
        m2mDataNode.setId("123");
        Map<String, Object> objectMap = ResourceReflection.serialize(m2mDataNode);
        System.out.println(objectMap.get("data"));

    }

    @Test
    public void testRunnable(){
        byte[] runnables=ResourceReflection.serializeKryo(new  MyRunnable());
        System.out.println("length:"+runnables.length);
        MyRunnable myRunnable= (MyRunnable) ResourceReflection.deserializeKryo(runnables);
        new Thread(myRunnable).start();
    }
}
