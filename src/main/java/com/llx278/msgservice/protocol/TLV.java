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

    private final int type;
    private final int len;
    private final ByteBuf value;

    public TLV(int type, int len, ByteBuf value) {
        this.type = type;
        this.len = len;
        this.value = value;
    }

    public int readType() {
        return type;
    }

    public int getLen() {
        return len;
    }

    public ByteBuf getValue() {
        return value;
    }

    public static void compositeTlvFrame(int type, BaseValue value, CompositeByteBuf dst, ByteBuf tl, ByteBuf v) {
        tl.writeInt(type);
        value.writeTo(v);
        tl.writeInt(v.readableBytes());
        dst.addComponents(true, tl, v);
    }

    public static void compositeTlvFrame(int type, ByteBuf v, ByteBuf dst) {
        dst.writeInt(type);
        dst.writeInt(v.readableBytes());
        dst.writeBytes(v);
    }

    public static boolean isHeartBeatFrame(ByteBuf buf) {
        int type = buf.getInt(0);
        return type == Type.FRAME_HEART;
    }

    public static boolean isRegisterFrame(ByteBuf buf) {
        int type = buf.getInt(0);
        return type == Type.FRAME_REGISTER;
    }

    public static boolean isMsgFrame(ByteBuf buf) {
        int type = buf.getInt(0);
        return type == Type.FRAME_MSG;
    }

    /**
     * 直接写入
     *
     * @param type type
     * @param dst  dst
     * @param tl   tl
     * @param v    v
     */
    public static void quickCompositeTlvFrame(int type, CompositeByteBuf dst, ByteBuf tl, ByteBuf v) {
        tl.writeInt(type);
        tl.writeInt(v.readableBytes());
        dst.addComponents(true, tl, v);
    }

    public static int readType(ByteBuf buf) {
        return buf.readInt();
    }

    public static int readLength(ByteBuf buf) {
        return buf.readInt();
    }

    /**
     * 注意，如果能够确定buf是原始的tlv格式，那么可以直接读到value，忽略t和l
     *
     * @param buf
     * @return
     */
    public static ByteBuf quickReadValue(ByteBuf buf) {
        buf.readInt();
        buf.readInt();
        return buf;
    }

    public static TLV unCompositeTlvFrame(ByteBuf tlvBuf) {

        int type = tlvBuf.readInt();
        int len = tlvBuf.readInt();
        if (len != tlvBuf.readableBytes()) {
            System.out.println("invalid tlv frame! expected len is " + len + " actual len is " + tlvBuf.readableBytes());
            return null;
        }
        ByteBuf v = ReferenceCountUtil.retain(tlvBuf);

        return new TLV(type, len, v);
    }
}
