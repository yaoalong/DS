package lab.mars.ds.register;

import lab.mars.ds.loadbalance.NetworkInterface;
import lab.mars.ds.register.starter.Starter;

import org.apache.zookeeper.server.quorum.QuorumPeerConfig.ConfigException;

/**
 * Author:yaoalong.
 * Date:2016/3/4.
 * Email:yaoalong@foxmail.com
 */
public class ZooKeeperRegister implements ServiceRegister {

    private Starter starter;

    @Override
    public void starter(String[] args, NetworkInterface networkInterface) {
        starter = new Starter(networkInterface);
        try {
            starter.mainStart(args);
        } catch (ConfigException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void register(String value) {
        System.out.println("注册");
        starter.register(value);
    }
}
