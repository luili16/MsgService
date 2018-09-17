package com.llx278.msgservice;

import com.llx278.msgservice.protocol.TLV;
import com.sun.jdi.ByteValue;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class InBufferHandler extends ChannelInboundHandlerAdapter {

    private static final Logger sLogger = LogManager.getLogger(InBufferHandler.class);
    public static final String NAME = "InBufferHandler";

    private List<ByteBuf> mAlreadyReadBufs = new LinkedList<>();

    private List<ByteBuf> mTempReadBufs = new LinkedList<>();

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        sLogger.log(Level.DEBUG, "channelRead");
        ByteBuf buf = (ByteBuf) msg;
        mTempReadBufs.add(buf);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        sLogger.log(Level.DEBUG, "channelReadComplete");

        CompositeByteBuf tempReadBuf = ctx.alloc().compositeBuffer(mTempReadBufs.size());
        tempReadBuf.addComponents(true, mTempReadBufs);

        // 循环读取
        while (true) {
            boolean hasFound = findSyncBytes(tempReadBuf);
            if (!hasFound) {
                sLogger.log(Level.ERROR,"没有发现sync字节 当前收到消息的长度 : " + tempReadBuf.readableBytes());
                feedLastBytesToTempReadBufs(ctx,tempReadBuf);
                ctx.read();
                break;
            }

            // 发现了一条消息
            int index = tempReadBuf.readableBytes();

            if (index < 12) {
                // 无法读到消息的长度,继续读
                feedLastBytesToTempReadBufs(ctx,tempReadBuf);
                ctx.read();
                break;
            }

            // 拿到消息的长度
            int len = tempReadBuf.getInt(4) + 8;
            // +4 是加FINISH字节
            if (tempReadBuf.readableBytes() < len + 4) {
                // 这条消息没有读完，接着读
                feedLastBytesToTempReadBufs(ctx,tempReadBuf);
                ctx.read();
                break;
            }

            // 接收到了一条完整的消息
            ByteBuf readingBuf = ctx.alloc().buffer(len);

            // 消耗掉SYNC字符
            drainSync(tempReadBuf);

            tempReadBuf.readBytes(readingBuf);
            // 读出了一条消息，那么判断后来的数据是不是FINISH字节，如果是
            // 那么就是说这条消息是有效的
            if (isFinishBytes(tempReadBuf)) {
                // 加入消息列表
                mAlreadyReadBufs.add(readingBuf);
                // 消耗掉结束字符
                drainFinish(tempReadBuf);
            } else {
                // 不是结束字符，那么就意味着这条消息读取失败了
                // 不需要消耗结束字符，继续读
                // 这里无法保证消息是始终都能收到的，虽然理论上是可以的
                // 出现这种情况就是网络出现了异常波动，所以需要更上层
                // 的协议来保证消息已经准确送达了
                sLogger.log(Level.ERROR,"读取到了一个无效的msg " + tempReadBuf);
                feedLastBytesToTempReadBufs(ctx,tempReadBuf);
                break;
            }
            // 一条消息已经读取结束了，那么就应该继续读下一条消息了,继续循环
        }

        // 将读到的消息发送给下一个handler执行
        if (mAlreadyReadBufs.isEmpty()) {
            sLogger.log(Level.ERROR,"empty already read bufs");
        }

        ctx.fireChannelRead(mAlreadyReadBufs);
        ctx.fireChannelReadComplete();
        mAlreadyReadBufs.clear();
    }

    /**
     * 找到一条消息的起始同步字节，在cubf中找到了同步的字节
     *
     * @param cbuf
     * @return true 找到了同步的字节，cbuf为包含同步字节的数据 false 没有找到 cbuf为无法判断的字节
     */
    private boolean findSyncBytes(ByteBuf cbuf) {
        while (cbuf.readableBytes() >= 4) {
            byte[] dst = new byte[4];
            int index = cbuf.readerIndex();
            dst[0] = cbuf.getByte(index);
            dst[1] = cbuf.getByte(index + 1);
            dst[2] = cbuf.getByte(index + 2);
            dst[3] = cbuf.getByte(index + 3);
            if (Arrays.equals(TLV.SYNC_BYTES, dst)) {
                sLogger.log(Level.DEBUG, "找到了一条msg的起始");
                return true;
            }
            // 消耗掉一个字节
            cbuf.readByte();
        }
        return false;
    }

    private boolean isFinishBytes(ByteBuf cbuf) {
        byte[] dst = new byte[4];
        int index = cbuf.readerIndex();
        dst[0] = cbuf.getByte(index);
        dst[1] = cbuf.getByte(index + 1);
        dst[2] = cbuf.getByte(index + 2);
        dst[3] = cbuf.getByte(index + 3);
        if (Arrays.equals(TLV.FINISH_BYTES, dst)) {
            sLogger.log(Level.DEBUG, "找到了一条msg的结束");
            return true;
        }
        return false;
    }

    private void feedLastBytesToTempReadBufs(ChannelHandlerContext ctx, ByteBuf tempReadBuf) {
        ByteBuf lastBytes = ctx.alloc().buffer();
        lastBytes.writeBytes(tempReadBuf);
        tempReadBuf.release();
        mTempReadBufs.clear();
        mTempReadBufs.add(lastBytes);
    }

    private void drainSync(ByteBuf tempReadBuf) {
        tempReadBuf.readByte();
        tempReadBuf.readByte();
        tempReadBuf.readByte();
        tempReadBuf.readByte();
    }

    private void drainFinish(ByteBuf tempReadBuf) {
        tempReadBuf.readByte();
        tempReadBuf.readByte();
        tempReadBuf.readByte();
        tempReadBuf.readByte();
    }
}
