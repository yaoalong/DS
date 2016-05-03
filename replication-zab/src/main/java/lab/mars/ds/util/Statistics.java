package lab.mars.ds.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Author:yaoalong.
 * Date:2016/5/3.
 * Email:yaoalong@foxmail.com
 */
public class Statistics {

    private AtomicLong atomicLong=new AtomicLong();
    public long get(){
        return atomicLong.get();
    }
    public void add(){
        atomicLong.getAndIncrement();
    }
}
