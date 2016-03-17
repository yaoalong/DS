package lab.mars.ds.network;

import lab.mars.ds.network.intialize.RegisterPacketServerChannelInitializer;
import lab.mars.ds.register.starter.Starter;

/**
 * Author:yaoalong.
 * Date:2016/3/17.
 * Email:yaoalong@foxmail.com
 */
public class RegisterTcpServer extends TcpServerNetwork{

    public RegisterTcpServer(Starter starter){
        setChannelChannelInitializer(new RegisterPacketServerChannelInitializer(
                starter));
    }
}
