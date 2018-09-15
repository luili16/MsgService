package com.llx278.msgservice;

import com.llx278.msgservice.protocol.RegisterFrame;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.Attribute;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class InRegisterHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger sLogger = LogManager.getLogger(InRegisterHandler.class);

    public static final String NAME = "InRegisterHandler";

    private ByteBuf buf;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        buf = msg;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {

        if (!RegisterFrame.isRegisterFrame(buf)) {
            buf = ReferenceCountUtil.retain(buf);
            ctx.fireChannelRead(buf);
            ctx.fireChannelReadComplete();
            return;
        }


        try {

            // 注册帧仅仅会是在socketchannel Active的时候发送一次
            RegisterFrame registerFrame = RegisterFrame.valueOf(buf);
            //ByteBuf reg = buf.getValue();
            int uid = registerFrame.getValue().getUid();
            sLogger.log(Level.DEBUG, "收到注册帧 uid : " + uid);
            Attribute<Map> attr = ctx.channel().attr(MsgServer.sSocketMapAttr);
            Map<Integer, SocketChannel> socketChannelMap = attr.get();

            // 先根据这个uid判断是否有一个对应的socketChannel
            SocketChannel socketChannel = socketChannelMap.get(uid);
            if (socketChannel == null) {
                socketChannelMap.put(uid, (SocketChannel) ctx.channel());
            } else {
                // 如果不是，那就意味着有其他的客户端想要用这uid来向服务器注册，
                // 这种情况下直接关闭老的socketChannel，并保存新的socketChannel
                sLogger.log(Level.ERROR, "关闭老的socketChannel 此时的uid是 : " + uid);
                socketChannel.close();
                socketChannelMap.put(uid, (SocketChannel) ctx.channel());
            }

            // 保存uid
            Attribute<Integer> uidAttr = ctx.channel().attr(MsgServer.sUidAttr);
            uidAttr.set(uid);

        } finally {
            if (buf.refCnt() != 0) {
                ReferenceCountUtil.release(buf, buf.refCnt());
            }
        }
    }
}
