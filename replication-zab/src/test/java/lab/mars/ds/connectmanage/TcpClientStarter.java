package lab.mars.ds.connectmanage;

import lab.mars.ds.network.TcpClientNetwork;
import lab.mars.ds.network.intialize.RegisterPacketClientChannelInitializer;

/**
 * Author:yaoalong.
 * Date:2016/3/8.
 * Email:yaoalong@foxmail.com
 */
public class TcpClientStarter {


    public static void main(String args[]){
        for(int i=0;i<20;i++){
            TcpClientNetwork tcpClient = new TcpClientNetwork();
            tcpClient
                    .setSocketChannelChannelInitializer(new RegisterPacketClientChannelInitializer());

            tcpClient.connectionOne("192.168.10.131",
                    Integer.valueOf(2181));
        }
    }
}
