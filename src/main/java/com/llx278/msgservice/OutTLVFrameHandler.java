package com.llx278.msgservice;

import com.llx278.msgservice.protocol.TLV;
import com.llx278.msgservice.protocol.Type;
import io.netty.buffer.ByteBuf;
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

        sLogger.log(Level.DEBUG, "准备写 封装TLV帧 msg : " + msg.toString());
        ByteBuf buf = (ByteBuf) msg;
        ByteBuf tlvBuf = TLV.composite(Type.FRAME_MSG, buf.readableBytes(), buf);
        ctx.write(tlvBuf);
        if (buf.refCnt() !=0) {
            ReferenceCountUtil.release(buf, buf.refCnt());
        }

    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
        sLogger.log(Level.DEBUG,"flush 结束");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        sLogger.log(Level.ERROR,cause);
        Helper.removeClient(ctx);
        ctx.close();
    }
}
