package com.llx278.msgservice;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.ResourceLeakDetector;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class MsgServer {

    public static final AttributeKey<Map> sSocketMapAttr = AttributeKey.valueOf("registerSocketMap");
    public static final AttributeKey<Integer> sUidAttr = AttributeKey.valueOf("uid");

    private static final Logger sLogger = LogManager.getLogger(MsgServer.class);

    // 读通道超时时间
    private static final int READ_IDLE_TIME = 60;
    private static final TimeUnit READ_IDLE_TIME_UNIT = TimeUnit.MINUTES;

    private int mPort;
    private Map<Integer,SocketChannel> mRegisterSocketMap = new ConcurrentHashMap<>();

    public MsgServer(int port) {
        this.mPort = port;
    }

    public void run() throws Exception {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            sLogger.log(Level.INFO,"接受一个连接 : " + ch.remoteAddress());
                            Attribute<Map> attr = ch.attr(sSocketMapAttr);
                            Map map = attr.get();
                            if (map == null) {
                                attr.set(mRegisterSocketMap);
                            }
                            ch.pipeline().addLast("IdleStateHandler",new IdleStateHandler(READ_IDLE_TIME,0,0,READ_IDLE_TIME_UNIT));
                            ch.pipeline().addLast(InBufferHandler.NAME,new InBufferHandler());
                            ch.pipeline().addLast(InRegisterHandler.NAME,new InRegisterHandler());
                            ch.pipeline().addLast(InHeartBeatHandler.NAME,new InHeartBeatHandler());
                            ch.pipeline().addLast(InMsgHandler.NAME,new InMsgHandler());
                            ch.pipeline().addLast(OutTLVFrameHandler.NAME,new OutTLVFrameHandler());
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.bind(mPort).sync(); // (7)

            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {

        sLogger.trace("进入应用程序");
        sLogger.log(Level.INFO,"启动应用");
        int port;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        } else {
            port = 12306;
        }
        new MsgServer(port).run();
        sLogger.log(Level.INFO,"退出应用");
        sLogger.trace("退出应用");
    }
}
