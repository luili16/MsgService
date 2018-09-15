package com.llx278.msgservice.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TLV {

    private static final Logger sLogger = LogManager.getLogger(TLV.class);

    private int mType = 0;
    private int mLength = 0;
    private ByteBuf mValue;

    public TLV(ByteBuf buf) {

        if (buf.readableBytes() < 8) {
            sLogger.log(Level.ERROR,"invalid buf");
            return;
        }

        mType = buf.readInt();
        mLength = buf.readInt();
        mValue = ReferenceCountUtil.retain(buf);
    }

    public int getType() {
        return mType;
    }

    public int getLength() {
        return mLength;
    }

    public ByteBuf getValue() {
        return mValue;
    }

    public static ByteBuf composite(int type,int length,ByteBuf value) {
        CompositeByteBuf buf = Unpooled.compositeBuffer();
        ByteBuf tl = Unpooled.buffer();
        tl.writeInt(type);
        tl.writeInt(length);
        buf.addComponents(true,tl,value);
        return buf;
    }
}
