package databus.network.netty;

import java.net.SocketAddress;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolMap;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;

public class NettyChannelPoolMap
                       extends AbstractChannelPoolMap<SocketAddress, FixedChannelPool> {

    public NettyChannelPoolMap(EventLoopGroup group) {
        this(group, 1);
    }

    public NettyChannelPoolMap(EventLoopGroup group, int maxConnections) {
        super();
        this.maxConnections = maxConnections;
        this.group = group;
    }

    @Override
    protected FixedChannelPool newPool(SocketAddress key) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                 .channel(NioSocketChannel.class)
                 .option(ChannelOption.TCP_NODELAY, true)
                 .option(ChannelOption.SO_KEEPALIVE, true)
                 .remoteAddress(key);

        FixedChannelPool pool = new FixedChannelPool(bootstrap,
                                                     databusChannelPoolHandler, 
                                                     maxConnections);
        return pool;
    }

    private int maxConnections;
    private EventLoopGroup group;
    private ChannelPoolHandler databusChannelPoolHandler = new NettyChannelPoolHandler();
}
