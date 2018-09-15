package com.llx278.msgservice;

import com.llx278.msgservice.protocol.Debug;
import com.llx278.msgservice.protocol.TLV;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

public class InBufferHandler extends ChannelInboundHandlerAdapter {

    private static final Logger sLogger = LogManager.getLogger(InBufferHandler.class);
    public static final String NAME = "InBufferHandler";

    private List<ByteBuf> mByteBufs = new LinkedList<>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        mByteBufs.add(buf);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {

        CompositeByteBuf compositeByteBuf = ctx.alloc().compositeBuffer(mByteBufs.size());
        compositeByteBuf.addComponents(true, mByteBufs);

        if (compositeByteBuf.readableBytes() == 0) {
            Attribute<Integer> uidAttr = ctx.channel().attr(MsgServer.sUidAttr);
            sLogger.log(Level.ERROR, "-------- EOF --------- 与 " + uidAttr.get() + " 的连接断开");
            Helper.removeClient(ctx);
            ReferenceCountUtil.release(compositeByteBuf, compositeByteBuf.refCnt());
            ctx.close();
            return;
        }

        ByteBuf copy = compositeByteBuf.copy();
        Debug.print(copy);

        compositeByteBuf = ReferenceCountUtil.retain(compositeByteBuf);
        ctx.fireChannelRead(compositeByteBuf);
        ctx.fireChannelReadComplete();
        mByteBufs.clear();
    }
}
