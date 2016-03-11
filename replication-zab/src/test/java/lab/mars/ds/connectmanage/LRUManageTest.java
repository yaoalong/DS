package lab.mars.ds.connectmanage;

import lab.mars.ds.network.TcpServer;

/**
 * Author:yaoalong. Date:2016/3/8. Email:yaoalong@foxmail.com
 */
public class LRUManageTest {

    public static void main(String args[]) {
        TcpServer tcpServer = new TcpServer(null, null,5);
        try {
            tcpServer.bind("192.168.10.131", 4444);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
