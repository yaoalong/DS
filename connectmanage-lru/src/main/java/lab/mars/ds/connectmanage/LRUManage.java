package lab.mars.ds.connectmanage;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Author:yaoalong.
 * Date:2016/3/3.
 * Email:yaoalong@foxmail.com
 */
public class LRUManage implements ConnectManager {


    private static int size = 16;
    /**
     * 利用了LinkedHashMap实现了LRU 连接池
     */
    public static final LinkedHashMap<Channel, Boolean> connectMessages= new LinkedHashMap<Channel, Boolean>(
            size, 0.5f, true) {
        /**
         *
         */
        private static final long serialVersionUID = 3033453005289310613L;

        @Override
        protected   boolean removeEldestEntry(
                Map.Entry<Channel, Boolean> eldest) {
            if (size() > size) {
                eldest.getKey().close();
                return true;
            }
            return false;
        }
    };

    public LRUManage(int size) {
        this.size = size;
    }

    @Override
    public   void refresh(Channel channel) {
        connectMessages.get(channel);
    }

    @Override
    public   void add(Channel channel) {
        connectMessages.put(channel,true);
    }
}
