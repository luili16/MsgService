package com.llx278.msgservice;

import com.llx278.msgservice.protocol.TLV;
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
    private ByteBuf mReadingBuf;
    private int mReadingLen;

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
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

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        sLogger.log(Level.DEBUG, "channelRead");
        ByteBuf buf = (ByteBuf) msg;
        mTempReadBufs.add(buf);
    }

    private void feedLastBytesToTempReadBufs(ChannelHandlerContext ctx, ByteBuf tempReadBuf) {
        ByteBuf lastBytes = ctx.alloc().buffer();
        lastBytes.writeBytes(tempReadBuf);
        tempReadBuf.release();
        mTempReadBufs.clear();
        mTempReadBufs.add(lastBytes);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        sLogger.log(Level.DEBUG, "channelReadComplete");

        CompositeByteBuf tempReadBuf = ctx.alloc().compositeBuffer(mTempReadBufs.size());
        tempReadBuf.addComponents(true, mTempReadBufs);

        if (mReadingBuf != null) {
            // 这种情况就是说上次有消息没有读完，那么继续读
            int noReadLen = mReadingLen - mReadingBuf.readableBytes();
            if (tempReadBuf.readableBytes() <= noReadLen) {
                mReadingBuf.writeBytes(tempReadBuf);
            }

            if (mReadingBuf.readableBytes() == mReadingLen) {
                // 读完了一条消息
                sLogger.log(Level.DEBUG,"读完了一条消息");
                //
                mAlreadyReadBufs.add(mReadingBuf);

            }

        }



        // 循环读取
        while (true) {

            boolean hasFound = findSyncBytes(tempReadBuf);
            if (!hasFound) {
                sLogger.log(Level.ERROR,"没有找到sync段 当前收到消息的长度 : " + tempReadBuf.readableBytes());
                feedLastBytesToTempReadBufs(ctx,tempReadBuf);
                ctx.read();
                break;
            }

            // 找到了消息段
            if (tempReadBuf.readableBytes() < 12) {
                // 无法读到消息的长度,继续读
                feedLastBytesToTempReadBufs(ctx,tempReadBuf);
                ctx.read();
                break;
            }

            // 消耗掉sync字节
            tempReadBuf.readByte();
            tempReadBuf.readByte();
            tempReadBuf.readByte();
            tempReadBuf.readByte();

            // 拿到消息的长度
            mReadingLen = tempReadBuf.getInt(4) + 8;
            mReadingBuf = ctx.alloc().buffer(mReadingLen);

            if (tempReadBuf.readableBytes() > mReadingLen + 4) {
                // 可以读了
            }

            tempReadBuf.readBytes(mReadingBuf);
            if (mReadingBuf.readableBytes() == mReadingLen) {
                if (tempReadBuf.readableBytes() < 4) {
                    // 这种情况需要继续读
                    feedLastBytesToTempReadBufs(ctx,tempReadBuf);
                    ctx.read();
                    break;
                } else {
                    // 判断后面跟的4个字节是不是结束字节
                    if (isFinishBytes(tempReadBuf)) {
                        // 是四个字节的结束，那么就意味着读取到了一个新的msg
                        mAlreadyReadBufs.add(mReadingBuf);
                        mReadingBuf = null;
                        // 消耗掉结束字节
                        tempReadBuf.readByte();
                        tempReadBuf.readByte();
                        tempReadBuf.readByte();
                        tempReadBuf.readByte();
                        // 继续尝试读下一条消息
                    }
                }
            } else {
                // 这种情况就是说这个消息在一次读取中没有完成，那么下次就应该接着读了
                // 清除上次读取的数据
                tempReadBuf.release();
                mTempReadBufs.clear();
            }
        }

        // 讲读到的消息发送给下一个handler执行
        if (mAlreadyReadBufs.isEmpty()) {
            sLogger.log(Level.ERROR,"empty already read bufs");
            return;
        }
        ctx.fireChannelRead(mAlreadyReadBufs);
        ctx.fireChannelReadComplete();
        mAlreadyReadBufs.clear();
    }
}
