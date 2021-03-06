package lab.mars.onem2m.test;

import org.junit.Test;
import org.lab.mars.onem2m.M2mKeeperException;
import org.lab.mars.onem2m.OneM2m;

/**
 * Author:yaoalong.
 * Date:2016/5/5.
 * Email:yaoalong@foxmail.com
 */
public class OneM2mTestDelete {
    /**
     * 222 850 699 906
     */
    @Test
    public void testDelete() {
        OneM2m oneM2m = new OneM2m("192.168.10.131:2183,192.168.10.131:2184,192.168.10.131:2185");
        String key = "/cse/ae";
        long startTime = System.nanoTime();
        for (int i = 0; i < 1; i++) {
            try {
                oneM2m.delete(key);
            } catch (M2mKeeperException e) {
                e.printStackTrace();
            }
        }
        System.out.println("cost time:" +String.format ("%,d",System.nanoTime() - startTime));
    }

}
