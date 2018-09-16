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

    private List<ByteBuf> bufList = new LinkedList<>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("channelRead");
        ByteBuf buf = (ByteBuf) msg;
        bufList.add(buf);
    }


    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channelReadComplete");
        CompositeByteBuf cb = ctx.alloc().compositeBuffer(bufList.size());
        cb.addComponents(true,bufList);

        try {
            if (cb.readableBytes() == 0) {
                sLogger.log(Level.ERROR,"-------- EOF --------- 与服务器的连接断开");
                ctx.close();
                return;
            }


            if (cb.readableBytes() < 8) {
                sLogger.log(Level.ERROR,"没有读到长度，继续读!!");
                ctx.read();
                return;
            }

            int len = cb.getInt(4);
            if (len != cb.readableBytes() - 8) {
                sLogger.log(Level.ERROR,"没有读完，继续读!!");
                ctx.read();
                return;
            }

            ctx.fireChannelRead(cb);
            ctx.fireChannelReadComplete();
        } finally {
            bufList.clear();
        }
    }
}
