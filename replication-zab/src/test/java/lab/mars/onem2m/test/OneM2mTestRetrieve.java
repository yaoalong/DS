package lab.mars.onem2m.test;

import org.junit.Test;
import org.lab.mars.onem2m.OneM2m;

import java.io.IOException;

/**
 * Author:yaoalong.
 * Date:2016/5/5.
 * Email:yaoalong@foxmail.com
 */
public class OneM2mTestRetrieve {
    /**
     * 读取内存中的数据tps达到数千
     */
    @Test
    public void testRetrieve() {
        OneM2m oneM2m = new OneM2m("192.168.10.131", 2185);
        String key = "ddd32f234234ds0";
        long startTime = System.nanoTime();
        for (int i = 0; i < 100000; i++) {
            oneM2m.getData(key);
        }
        System.out.println("cost time:" + (System.nanoTime() - startTime));
    }
}
