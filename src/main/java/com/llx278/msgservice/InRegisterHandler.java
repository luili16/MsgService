package com.llx278.msgservice;

import com.llx278.msgservice.protocol.RegisterValue;
import com.llx278.msgservice.protocol.TLV;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.Attribute;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class InRegisterHandler extends ChannelInboundHandlerAdapter {

    private static final Logger sLogger = LogManager.getLogger(InRegisterHandler.class);

    public static final String NAME = "InRegisterHandler";

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;

        if (!TLV.isRegisterFrame(buf)) {
            ctx.fireChannelRead(buf);
            ctx.fireChannelReadComplete();
            return;
        }

        try {

            // 注册帧仅仅会是在socketchannel Active的时候发送一次
            TLV.readType(buf);
            int len = TLV.readLength(buf);
            int actualLen = buf.readableBytes();
            if (len != actualLen) {
                sLogger.log(Level.ERROR,"invalid register frame expected length is " + len + " but actual length is " + actualLen);
                return;
            }
            int uid = RegisterValue.quickReadUid(buf);
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
            sLogger.log(Level.DEBUG,"buf count : before" + buf.refCnt());
            buf.release(buf.refCnt());
            sLogger.log(Level.DEBUG,"buf count : after" + buf.refCnt());
        }
    }


}
