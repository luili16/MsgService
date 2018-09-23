package com.llx278.msgservice;

import com.llx278.msgservice.protocol.TLV;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.Attribute;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;


public class InMsgHandler extends ChannelInboundHandlerAdapter {

    private static final Logger sLogger = LogManager.getLogger(InMsgHandler.class);

    public static final String NAME = "InMsgHandler";

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        if (!TLV.isMsgFrame(buf)) {
            sLogger.log(Level.ERROR, "unKnow Type : " + buf.readInt());
            ReferenceCountUtil.release(buf, buf.refCnt());
            Helper.removeClient(ctx);
            ctx.close();
            return;
        }
        // 直接剥离tlv frame，效率更高
        //BaseFrame.stripTL(mBuf);
        TLV.readType(buf);
        int len = TLV.readLength(buf);
        if (len != buf.readableBytes()) {
            sLogger.log(Level.ERROR,"invalid tlv frame! expected len is " + len + " but actual len is " + buf.readableBytes());
            buf.release(buf.refCnt());
            return;
        }

        int fromUid = buf.getInt(buf.readerIndex());
        int toUid = buf.getInt(buf.readerIndex() + 4);
        Attribute<Map> attr = ctx.channel().attr(MsgServer.sSocketMapAttr);
        Map<Integer, SocketChannel> socketChannelMap = attr.get();
        if (socketChannelMap == null) {
            sLogger.log(Level.ERROR, "socketChannelMap is null !!!!!");
            buf.release(buf.refCnt());
            return;
        }
        SocketChannel channel = socketChannelMap.get(toUid);
        if (channel == null) {
            sLogger.log(Level.ERROR, "没有找到对应 uid的客户端 : " + toUid);
            buf.release(buf.refCnt());
            return;
        }
        sLogger.log(Level.DEBUG,"路由消息 from " + fromUid + " to " + toUid);
        ChannelFuture future = channel.writeAndFlush(buf);
        future.addListener(f -> {

            if (f.cause() != null) {
                sLogger.log(Level.ERROR, f.cause().getMessage());
            }
        });

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        sLogger.info(Level.ERROR,cause);
        Helper.removeClient(ctx);
        ctx.close();
    }
}
