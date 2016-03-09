package lab.mars.ds.network;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

/**
 *
 */
public class NetworkEventLoopGroup {

    public static final int NCPU = Runtime.getRuntime().availableProcessors();

    public static final EventLoopGroup bossGroup;

    public static final EventLoopGroup workerGroup;

    static {
        bossGroup = new NioEventLoopGroup(NCPU);
        workerGroup = new NioEventLoopGroup(NCPU);
    }

    public static void shutdown() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
