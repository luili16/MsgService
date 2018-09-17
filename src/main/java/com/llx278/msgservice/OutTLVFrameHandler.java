package com.llx278.msgservice;

import com.llx278.msgservice.protocol.Debug;
import com.llx278.msgservice.protocol.TLV;
import com.llx278.msgservice.protocol.Type;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.SocketAddress;

public class OutTLVFrameHandler extends ChannelOutboundHandlerAdapter {

    private static final Logger sLogger = LogManager.getLogger(OutTLVFrameHandler.class);
    public static final String NAME = "OutTLVFrameHandler";

    public OutTLVFrameHandler() {
        super();
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        ctx.connect(remoteAddress, localAddress, promise);
        sLogger.info("connect -> remoteAddress : " + remoteAddress.toString() + " localAddress : " + localAddress.toString());
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        ByteBuf dst = ctx.alloc().buffer();
        TLV.compositeTlvFrame(Type.FRAME_MSG, buf, dst);
        ByteBuf sync = ctx.alloc().buffer();
        ByteBuf finish = ctx.alloc().buffer();
        sync.writeInt(TLV.SYNC);
        finish.writeInt(TLV.FINISH);
        ctx.write(sync);
        ctx.write(dst);
        ctx.write(finish);
        ctx.flush();
    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        sLogger.log(Level.ERROR, cause);
        Helper.removeClient(ctx);
        ctx.close();
    }
}
