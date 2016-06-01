package lab.mars.onem2m.test;

import org.junit.Test;
import org.lab.mars.onem2m.M2mKeeperException;
import org.lab.mars.onem2m.OneM2m;

/**
 * Author:yaoalong.
 * Date:2016/5/5.
 * Email:yaoalong@foxmail.com
 */
public class OneM2mTestSetData {

    /**
     * 354 190 548 492
     */
    @Test
    public void testSetData() {
        OneM2m oneM2m = new OneM2m("192.168.10.131:2183,192.168.10.131:2184,192.168.10.131:2185");
        String key = "/cse/ae3";
        long startTime = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            try {
                oneM2m.setData(key+i , "111".getBytes());
            } catch (M2mKeeperException e) {
                e.printStackTrace();
            }
        }
        System.out.println("cost time:" +String.format ("%,d",System.nanoTime() - startTime));
    }
}
