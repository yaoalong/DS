package org.lab.mars.onem2m.server.util;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

/**
 * Author:yaoalong.
 * Date:2016/2/26.
 * Email:yaoalong@foxmail.com
 */
public class PacketChannelIntializerOperate {

    public static  void initChannel(SocketChannel channel){
        ChannelPipeline channelPipeline = channel.pipeline();
        channelPipeline.addLast(new ObjectEncoder());
        channelPipeline.addLast(new ObjectDecoder(ClassResolvers
                .cacheDisabled(null)));

    }
}


