package com.llx278.msgservice;

import com.llx278.msgservice.protocol.HeartBeatValue;
import com.llx278.msgservice.protocol.TLV;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class InHeartBeatHandler extends ChannelInboundHandlerAdapter {

    private static final Logger sLogger = LogManager.getLogger(InHeartBeatHandler.class);

    public static final String NAME = "InHeartBeatHandler";


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;

        if (!TLV.isHeartBeatFrame(buf)) {
            ctx.fireChannelRead(buf);
            ctx.fireChannelReadComplete();
            return;
        }
        try {

            // 心跳帧
            TLV.readType(buf);
            int len = TLV.readLength(buf);
            int readableBytes = buf.readableBytes();
            if (len != readableBytes) {
                sLogger.log(Level.ERROR,"invalid heart beat frame expect length is " + len + " but actual is " + readableBytes);
                return;
            }
            int uid = HeartBeatValue.quickReadUid(buf);
            Attribute<Integer> uidAttr = ctx.channel().attr(MsgServer.sUidAttr);
            Integer savedUid = uidAttr.get();

            if (savedUid != null && savedUid != uid) {
                // 这种情况是一个异常，因为一个socketChannel对应
                // 于一个uid，发生这种情况只能是心跳帧的uid发送错了
                // 那么我就直接关闭ChannelSocket
                sLogger.log(Level.ERROR,"发现心跳异常");
                ctx.close();
                Attribute<Map> mapAttribute = ctx.channel().attr(MsgServer.sSocketMapAttr);
                Map map = mapAttribute.get();
                map.remove(savedUid);
            }

        } finally {
            sLogger.log(Level.DEBUG,"buf count : before" + buf.refCnt());
            buf.release(buf.refCnt());
            sLogger.log(Level.DEBUG,"buf count : after" + buf.refCnt());
        }

    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                sLogger.log(Level.ERROR,ctx.channel().remoteAddress() + " 心跳异常");
                ctx.close();
            }
            return;
        }
        super.userEventTriggered(ctx, evt);
    }
}
