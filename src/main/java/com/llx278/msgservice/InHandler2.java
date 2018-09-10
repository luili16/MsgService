package com.llx278.msgservice;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import sun.plugin2.message.ReleaseRemoteObjectMessage;

public class InHandler2 extends ChannelInboundHandlerAdapter {

    public InHandler2() {
        System.out.println("channel handler2 init");
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        System.out.println("handler2 register");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //super.channelRead(ctx, msg);
        System.out.println("msg read handler2 ctx : " + ctx.toString());
        ReferenceCountUtil.release(msg);
    }
}
