package com.llx278.msgservice;

import com.llx278.msgservice.protocol.HeartBeatFrame;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class InHeartBeatHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger sLogger = LogManager.getLogger(InHeartBeatHandler.class);

    public static final String NAME = "InHeartBeatHandler";
    private ByteBuf buf;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        buf = msg;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {

        if (!HeartBeatFrame.isHeartBeatFrame(buf)) {
            buf = ReferenceCountUtil.retain(buf);
            ctx.fireChannelRead(buf);
            ctx.fireChannelReadComplete();
            return;
        }

        try {
            // 心跳帧
            HeartBeatFrame heartBeatFrame = HeartBeatFrame.valueOf(buf);
            HeartBeatFrame.Value value = heartBeatFrame.getValue();
            int uid = value.getUid();

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
            if (buf.refCnt() != 0) {
                ReferenceCountUtil.release(buf, buf.refCnt());
            }
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
