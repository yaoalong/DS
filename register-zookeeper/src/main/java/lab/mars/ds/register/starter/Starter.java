package lab.mars.ds.register.starter;

import java.io.IOException;

import lab.mars.ds.loadbalance.NetworkInterface;
import lab.mars.ds.loadbalance.impl.NetworkPool;
import lab.mars.ds.monitor.RegisterIntoZooKeeper;
import lab.mars.ds.monitor.ZooKeeper_Monitor;
import lab.mars.ds.network.TcpClientNetwork;
import lab.mars.ds.network.TcpServerNetwork;
import lab.mars.ds.network.intialize.RegisterPacketClientChannelInitializer;
import lab.mars.ds.network.intialize.RegisterPacketServerChannelInitializer;
import lab.mars.ds.register.model.RegisterM2mPacket;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig.ConfigException;
import org.apache.zookeeper.server.quorum.QuorumPeerMain;

public class Starter {

    public volatile static int count = 0;
    private QuorumPeerMain quorumPeerMain = new QuorumPeerMain();
    private NetworkInterface networkInterface;
    private NetworkPool networkPool;
    private String myServer;
    private Integer startFactor;
    private String zooKeeperServer;
    private ZooKeeper zooKeeper;

    public Starter(NetworkInterface networkInterface) {
        this.networkInterface = networkInterface;
    }

    public void startServer() {

        QuorumPeerConfig config = quorumPeerMain.getConfig();
        zooKeeperServer = config.getZooKeeperServer();
        networkPool = new NetworkPool();
        networkPool.setAllServers(config.getAllServers());
        startFactor = config.getZooKeeperStartFactor();
        TcpServerNetwork tcpServer = new TcpServerNetwork();
        tcpServer
                .setChannelChannelInitializer(new RegisterPacketServerChannelInitializer(
                        this));
        myServer = config.getMyIp();
        String[] serverAndPort = spilitString(myServer);
        try {
            tcpServer.bind(serverAndPort[0], Integer.valueOf(serverAndPort[1]));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void mainStart(String args[]) throws ConfigException {
        quorumPeerMain.parse(args);
        startServer();

        if (networkPool.getFirstPosition(myServer) < startFactor) {

            try {
                zooKeeper = new ZooKeeper(zooKeeperServer, 30000, null);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            start();
            check();

        } else {

            try {
                zooKeeper = new ZooKeeper(zooKeeperServer, 30000, null);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            check();
        }

    }

    public void check() {
        try {
            Thread.sleep(2000);

            while (true) {
                try {
                    zooKeeper = new ZooKeeper(zooKeeperServer, 10000, null);
                    System.out.println("开始");
                    zooKeeper.getChildren("/server", null);
                    System.out.println("成功");
                    return;
                } catch (Exception e) {

                    System.out.println("错误信息" + e.getMessage());
                    break;
                }

            }
            // for (String server : networkPool.getBeforeList(myServer)) {
            // try {
            // TcpClientNetwork tcpClient = new TcpClientNetwork();
            // tcpClient
            // .setSocketChannelChannelInitializer(new
            // RegisterPacketClientChannelInitializer());
            // String[] serverAndPort = spilitString(server);
            //
            // tcpClient.connectionOne(serverAndPort[0],
            // Integer.valueOf(serverAndPort[1]));
            // tcpClient.write(new RegisterM2mPacket(1, null));
            // } catch (Exception ex) {
            // }
            // }

            Thread.sleep(1000);
            if (count == 0) {
                start();
            } else {
                check();
            }
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

    }

    public void start() {

        startNextServers();
        // quorumPeerMain.runFromConfig(quorumPeerMain.getConfig());
        new Thread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                try {
                    quorumPeerMain.runFromConfig(quorumPeerMain.getConfig());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }).start();

    }

    public void startNextServers() {
        for (String server : networkPool.getNextServers(myServer,
                startFactor - 1)) {
            try {
                TcpClientNetwork tcpClient = new TcpClientNetwork();
                tcpClient
                        .setSocketChannelChannelInitializer(new RegisterPacketClientChannelInitializer());
                String[] serverAndPort = spilitString(server);

                tcpClient.connectionOne(serverAndPort[0],
                        Integer.valueOf(serverAndPort[1]));
                tcpClient.write(new RegisterM2mPacket(0, null));
            } catch (Exception ex) {
            }
        }
    }

    /*
     * 将server拆分为ip以及port
     */
    private String[] spilitString(String ip) {
        String[] splitMessage = ip.split(":");
        return splitMessage;
    }

    public void register(String value) {
        System.out.println("开始注册");
        RegisterIntoZooKeeper registerIntoZooKeeper = new RegisterIntoZooKeeper();
        try {
            registerIntoZooKeeper.setServer(zooKeeperServer);
            registerIntoZooKeeper.register(value);

            registerIntoZooKeeper.start();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            registerIntoZooKeeper.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ZooKeeper_Monitor zooKeeper_monitor = new ZooKeeper_Monitor();
        zooKeeper_monitor.setServer(zooKeeperServer);
        zooKeeper_monitor.setNetworkPool(networkInterface);
        zooKeeper_monitor.start();

    }

}
