package lab.mars.ds.connectmanage;

import lab.mars.ds.network.TcpServerNetwork;
import lab.mars.ds.network.intialize.RegisterPacketServerChannelInitializer;

/**
 * Author:yaoalong.
 * Date:2016/3/8.
 * Email:yaoalong@foxmail.com
 */
public class LRUManageTest {


    public static void main(String args[]){
        TcpServerNetwork tcpServer = new TcpServerNetwork();
        tcpServer
                .setChannelChannelInitializer(new RegisterPacketServerChannelInitializer(
                        null));
        try {
            tcpServer.bind("192.168.10.131", 2181);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
