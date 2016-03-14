package lab.mars.ds.network.intialize;

import io.netty.channel.ChannelPipeline;
import lab.mars.ds.network.handler.RegisterPacketServerChannelHandler;
import lab.mars.ds.network.initializer.TcpChannelInitializer;
import lab.mars.ds.register.starter.Starter;

public class RegisterPacketServerChannelInitializer extends
        TcpChannelInitializer {
    private Starter register;

    public RegisterPacketServerChannelInitializer(Starter register) {
        this.register = register;
    }


    @Override
    public void init(ChannelPipeline ch) {
        ch.addLast(new RegisterPacketServerChannelHandler(register));
    }
}