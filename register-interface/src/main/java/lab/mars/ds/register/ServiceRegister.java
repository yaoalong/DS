package lab.mars.ds.register;

import lab.mars.ds.loadbalance.LoadBalanceException;
import lab.mars.ds.loadbalance.NetworkInterface;

/**
 * Author:yaoalong. Date:2016/3/3. Email:yaoalong@foxmail.com
 */
public interface ServiceRegister {
    /**
     * 开启服务
     * 
     * @param args
     */
    void starter(String args[], NetworkInterface networkInterface)
            throws LoadBalanceException;

    /**
     * 注册对应的value
     * 
     * @param value
     */
    void register(String value);
}
