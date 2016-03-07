package lab.mars.ds.connectmanage;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.junit.Test;

import java.net.SocketAddress;

/**
 * Author:yaoalong.
 * Date:2016/3/5.
 * Email:yaoalong@foxmail.com
 */
public class LRUManageTest {

    @Test
    public void test() {
        LRUManage lruManage = new LRUManage(16);
        for (int i = 0; i < 16; i++) {
            lruManage.add(new Channel() {
                @Override
                public EventLoop eventLoop() {
                    return null;
                }

                @Override
                public Channel parent() {
                    return null;
                }

                @Override
                public ChannelConfig config() {
                    return null;
                }

                @Override
                public boolean isOpen() {
                    return false;
                }

                @Override
                public boolean isRegistered() {
                    return false;
                }

                @Override
                public boolean isActive() {
                    return false;
                }

                @Override
                public ChannelMetadata metadata() {
                    return null;
                }

                @Override
                public SocketAddress localAddress() {
                    return null;
                }

                @Override
                public SocketAddress remoteAddress() {
                    return null;
                }

                @Override
                public ChannelFuture closeFuture() {
                    return null;
                }

                @Override
                public boolean isWritable() {
                    return false;
                }

                @Override
                public Unsafe unsafe() {
                    return null;
                }

                @Override
                public ChannelPipeline pipeline() {
                    return null;
                }

                @Override
                public ByteBufAllocator alloc() {
                    return null;
                }

                @Override
                public ChannelPromise newPromise() {
                    return null;
                }

                @Override
                public ChannelProgressivePromise newProgressivePromise() {
                    return null;
                }

                @Override
                public ChannelFuture newSucceededFuture() {
                    return null;
                }

                @Override
                public ChannelFuture newFailedFuture(Throwable cause) {
                    return null;
                }

                @Override
                public ChannelPromise voidPromise() {
                    return null;
                }

                @Override
                public ChannelFuture bind(SocketAddress localAddress) {
                    return null;
                }

                @Override
                public ChannelFuture connect(SocketAddress remoteAddress) {
                    return null;
                }

                @Override
                public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
                    return null;
                }

                @Override
                public ChannelFuture disconnect() {
                    return null;
                }

                @Override
                public ChannelFuture close() {
                    System.out.println("关闭dfsdfsdf");
                    return null;
                }

                @Override
                public ChannelFuture deregister() {
                    return null;
                }

                @Override
                public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
                    return null;
                }

                @Override
                public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
                    return null;
                }

                @Override
                public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
                    return null;
                }

                @Override
                public ChannelFuture disconnect(ChannelPromise promise) {
                    return null;
                }

                @Override
                public ChannelFuture close(ChannelPromise promise) {
                    System.out.println("关闭dfsdfsdf");
                    return null;
                }

                @Override
                public ChannelFuture deregister(ChannelPromise promise) {
                    return null;
                }

                @Override
                public Channel read() {
                    return null;
                }

                @Override
                public ChannelFuture write(Object msg) {
                    return null;
                }

                @Override
                public ChannelFuture write(Object msg, ChannelPromise promise) {
                    return null;
                }

                @Override
                public Channel flush() {
                    return null;
                }

                @Override
                public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
                    return null;
                }

                @Override
                public ChannelFuture writeAndFlush(Object msg) {
                    return null;
                }

                @Override
                public <T> Attribute<T> attr(AttributeKey<T> key) {
                    return null;
                }

                @Override
                public int compareTo(Channel o) {
                    return 0;
                }
            });
        }
        System.out.println("OK");
        lruManage.add(new Channel() {
            @Override
            public EventLoop eventLoop() {
                return null;
            }

            @Override
            public Channel parent() {
                return null;
            }

            @Override
            public ChannelConfig config() {
                return null;
            }

            @Override
            public boolean isOpen() {
                return false;
            }

            @Override
            public boolean isRegistered() {
                return false;
            }

            @Override
            public boolean isActive() {
                return false;
            }

            @Override
            public ChannelMetadata metadata() {
                return null;
            }

            @Override
            public SocketAddress localAddress() {
                return null;
            }

            @Override
            public SocketAddress remoteAddress() {
                return null;
            }

            @Override
            public ChannelFuture closeFuture() {
                return null;
            }

            @Override
            public boolean isWritable() {
                return false;
            }

            @Override
            public Unsafe unsafe() {
                return null;
            }

            @Override
            public ChannelPipeline pipeline() {
                return null;
            }

            @Override
            public ByteBufAllocator alloc() {
                return null;
            }

            @Override
            public ChannelPromise newPromise() {
                return null;
            }

            @Override
            public ChannelProgressivePromise newProgressivePromise() {
                return null;
            }

            @Override
            public ChannelFuture newSucceededFuture() {
                return null;
            }

            @Override
            public ChannelFuture newFailedFuture(Throwable cause) {
                return null;
            }

            @Override
            public ChannelPromise voidPromise() {
                return null;
            }

            @Override
            public ChannelFuture bind(SocketAddress localAddress) {
                return null;
            }

            @Override
            public ChannelFuture connect(SocketAddress remoteAddress) {
                return null;
            }

            @Override
            public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
                return null;
            }

            @Override
            public ChannelFuture disconnect() {
                return null;
            }

            @Override
            public ChannelFuture close() {
                return null;
            }

            @Override
            public ChannelFuture deregister() {
                return null;
            }

            @Override
            public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
                return null;
            }

            @Override
            public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
                return null;
            }

            @Override
            public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
                return null;
            }

            @Override
            public ChannelFuture disconnect(ChannelPromise promise) {
                return null;
            }

            @Override
            public ChannelFuture close(ChannelPromise promise) {
                System.out.println("haha");
                return null;
            }

            @Override
            public ChannelFuture deregister(ChannelPromise promise) {
                return null;
            }

            @Override
            public Channel read() {
                return null;
            }

            @Override
            public ChannelFuture write(Object msg) {
                return null;
            }

            @Override
            public ChannelFuture write(Object msg, ChannelPromise promise) {
                return null;
            }

            @Override
            public Channel flush() {
                return null;
            }

            @Override
            public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
                return null;
            }

            @Override
            public ChannelFuture writeAndFlush(Object msg) {
                return null;
            }

            @Override
            public <T> Attribute<T> attr(AttributeKey<T> key) {
                return null;
            }

            @Override
            public int compareTo(Channel o) {
                return 0;
            }
        });
    }
}
