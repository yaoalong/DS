package lab.mars.ds.connectmanage;

import io.netty.channel.Channel;

/**
 * Author:yaoalong.
 * Date:2016/3/3.
 * Email:yaoalong@foxmail.com
 */

/**
 * 连接管理
 */
public interface ConnectManager {

    void refresh(Channel channel);

    void add(Channel channel);
}
