package com.llx278.msgservice.protocol;

import io.netty.buffer.ByteBuf;

public abstract class BaseFrame<T extends BaseFrame.BaseValue> {

    protected final int type;
    protected final T value;

    public BaseFrame(int type, T value) {
        this.type = type;
        this.value = value;
    }

    public int getType() {
        return type;
    }

    public T getValue() {
        return value;
    }

    public static void stripTL(ByteBuf buf) {
        buf.readInt();
        buf.readInt();
    }

    public abstract void writeToByteBuf(ByteBuf buf);

    public abstract static class BaseValue {

        public abstract int length();
        public abstract void writeTo(ByteBuf outByteBuf);

    }
}
