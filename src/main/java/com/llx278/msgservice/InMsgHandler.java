package com.llx278.msgservice;

import com.llx278.msgservice.protocol.BaseFrame;
import com.llx278.msgservice.protocol.MsgFrame;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.Attribute;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;


public class InMsgHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger sLogger = LogManager.getLogger(InMsgHandler.class);

    public static final String NAME = "InMsgHandler";

    private ByteBuf mBuf;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        mBuf = msg;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {

        if (!MsgFrame.isMsgFrame(mBuf)) {
            sLogger.log(Level.ERROR, "unKnow Type : " + mBuf.readInt());
            ReferenceCountUtil.release(mBuf, mBuf.refCnt());
            Helper.removeClient(ctx);
            ctx.close();
        }
        // 直接剥离tlv frame，效率更高
        //BaseFrame.stripTL(mBuf);
        mBuf.readInt();
        mBuf.readInt();
        int fromUid = mBuf.getInt(mBuf.readerIndex());
        int toUid = mBuf.getInt(mBuf.readerIndex() + 4);
        Attribute<Map> attr = ctx.channel().attr(MsgServer.sSocketMapAttr);
        Map<Integer, SocketChannel> socketChannelMap = attr.get();
        if (socketChannelMap == null) {
            sLogger.log(Level.ERROR, "socketChannelMap is null !!!!!");
            return;
        }
        SocketChannel channel = socketChannelMap.get(toUid);
        if (channel == null) {
            sLogger.log(Level.ERROR, "没有找到对应 uid的客户端 : " + toUid);
            return;
        }
        sLogger.log(Level.DEBUG,"路由消息 from " + fromUid + " to " + toUid);
        ChannelFuture future = channel.writeAndFlush(mBuf);
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
