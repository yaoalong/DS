package lab.mars.onem2m.test;

import org.junit.Test;
import org.lab.mars.onem2m.OneM2m;

import java.io.IOException;

/**
 * Author:yaoalong.
 * Date:2016/5/5.
 * Email:yaoalong@foxmail.com
 */
public class OneM2mCreateTest {


    @Test
    public void testCreate() {
        OneM2m oneM2m = new OneM2m("192.168.10.131", 2184);
        String key = "/cse/ae";
        long startTime = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            try {
                oneM2m.create(key, "111".getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("cost time:" +String.format ("%,d",System.nanoTime() - startTime));
    }
}


