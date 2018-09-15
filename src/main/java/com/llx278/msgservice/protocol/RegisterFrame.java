package com.llx278.msgservice.protocol;

import io.netty.buffer.ByteBuf;

public class RegisterFrame extends BaseFrame<RegisterFrame.Value> {

    public RegisterFrame(int type,Value value) {
        super(type, value);
    }

    @Override
    public void writeToByteBuf(ByteBuf buf) {
        buf.writeInt(type);
        buf.writeInt(value.length());
        value.writeTo(buf);
    }

    public static RegisterFrame valueOf(ByteBuf buf) {
        int type = buf.readInt();
        int len = buf.readInt();
        Value value = new Value(buf.readInt());
        if (len != value.length()) {
            throw new IllegalStateException("invalid tlv frame");
        }
        return new RegisterFrame(type,value);
    }

    public static boolean isRegisterFrame(ByteBuf buf) {
        int type = buf.getInt(0);
        return type == Type.FRAME_REGISTER;
    }

    public static class Value extends BaseFrame.BaseValue {

        private final int uid;

        public Value(int uid) {
            this.uid = uid;
        }

        @Override
        public int length() {
            return 4;
        }

        public int getUid() {
            return uid;
        }

        @Override
        public void writeTo(ByteBuf outByteBuf) {
            outByteBuf.writeInt(uid);
        }
    }
}
