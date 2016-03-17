package lab.mars.ds.register.starter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lab.mars.ds.constant.OperateConstant;
import lab.mars.ds.loadbalance.NetworkInterface;
import lab.mars.ds.loadbalance.impl.NetworkPool;
import lab.mars.ds.monitor.RegisterIntoZooKeeper;
import lab.mars.ds.monitor.ZooKeeper_Monitor;
import lab.mars.ds.network.RegisterTcpClient;
import lab.mars.ds.network.TcpServerNetwork;
import lab.mars.ds.network.intialize.RegisterPacketClientChannelInitializer;
import lab.mars.ds.network.intialize.RegisterPacketServerChannelInitializer;
import lab.mars.ds.register.constant.RegisterConstant;
import lab.mars.ds.register.model.RegisterM2mPacket;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig.ConfigException;
import org.apache.zookeeper.server.quorum.QuorumPeerMain;

public class Starter {

    public static List<String> servers = new ArrayList<String>();
    private QuorumPeerMain quorumPeerMain = new QuorumPeerMain();
    private NetworkInterface networkInterface;
    private NetworkPool networkPool;
    private String myServer;
    private Integer startFactor;
    private String zooKeeperServer;
    private ZooKeeper zooKeeper;

    private volatile boolean isStarted = false;

    public Starter(NetworkInterface networkInterface) {
        this.networkInterface = networkInterface;
    }

    /**
     * 开始启动server 包括启动tcpserver,接收来自客户端的请求
     * 
     */
    public void startServer() {

        QuorumPeerConfig config = quorumPeerMain.getConfig();
        zooKeeperServer = config.getZooKeeperServer();
        try {
            zooKeeper = new ZooKeeper(zooKeeperServer,
                    RegisterConstant.SESSION_TIME, null);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
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
            throw e;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void mainStart(String args[]) throws ConfigException {
        quorumPeerMain.parse(args);
        startServer();
        check();

    }

    public void check() {
        try {
            Thread.sleep(RegisterConstant.SLEEP_TIME);

            while (true) {
                try {
                    zooKeeper.create("/server", "1".getBytes(),
                            Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                    return;
                } catch (Exception e) {
                    if (e instanceof KeeperException.NodeExistsException) {
                        return;
                    }
                    e.printStackTrace();
                    break;
                }

            }
            for (String server : networkPool.getBeforeList(myServer)) {
                try {
                    RegisterTcpClient tcpClient = new RegisterTcpClient();
                    tcpClient
                            .setSocketChannelChannelInitializer(new RegisterPacketClientChannelInitializer());
                    String[] serverAndPort = spilitString(server);

                    tcpClient.connectionOne(serverAndPort[0],
                            Integer.valueOf(serverAndPort[1]));
                    tcpClient.write(new RegisterM2mPacket(
                            OperateConstant.DETECT.getCode(), null));
                } catch (Exception ex) {
                }
            }

            Thread.sleep(RegisterConstant.SLEEP_TIME);
            synchronized (servers) {
                if (servers.size() < startFactor) {
                    start();
                }
            }
            check();
        } catch (InterruptedException e1) {
        }

    }

    public void start() {
        if (isStarted == true) {
            return;
        }
        isStarted = true;
        startNextServers();
        new Thread(() -> {
            try {
                quorumPeerMain.runFromConfig(quorumPeerMain.getConfig());
            } catch (IOException e) {
                e.printStackTrace();
            }

        }).start();

    }

    public void startNextServers() {
        for (String server : networkPool.getNextServers(myServer,
                startFactor - 1)) {
            try {
                RegisterTcpClient tcpClient = new RegisterTcpClient();
                tcpClient
                        .setSocketChannelChannelInitializer(new RegisterPacketClientChannelInitializer());
                String[] serverAndPort = spilitString(server);

                tcpClient.connectionOne(serverAndPort[0],
                        Integer.valueOf(serverAndPort[1]));
                tcpClient.write(new RegisterM2mPacket(OperateConstant.START
                        .getCode(), null));
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

        RegisterIntoZooKeeper registerIntoZooKeeper = new RegisterIntoZooKeeper(
                this);
        try {
            registerIntoZooKeeper.setServer(zooKeeperServer);
            registerIntoZooKeeper.register(value);

            registerIntoZooKeeper.start();
        } catch (IOException e) {

        } catch (KeeperException e) {

        } catch (InterruptedException e) {
        }
        try {
            registerIntoZooKeeper.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ZooKeeper_Monitor zooKeeper_monitor = new ZooKeeper_Monitor(this);
        zooKeeper_monitor.setZooKeeperServer(zooKeeperServer);
        zooKeeper_monitor.setNetworkPool(networkInterface);
        zooKeeper_monitor.start();

    }

    public String getMyServer() {
        return myServer;
    }

}
