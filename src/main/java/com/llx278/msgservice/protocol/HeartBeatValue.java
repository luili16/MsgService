package com.llx278.msgservice.protocol;

import io.netty.buffer.ByteBuf;

public class HeartBeatValue extends BaseValue {

    private int uid;

    public HeartBeatValue(int uid) {
        this.uid = uid;
    }

    public int getUid() {
        return uid;
    }

    @Override
    public void writeTo(ByteBuf dst) {
        dst.writeInt(this.uid);
    }

    /**
     * 快速写入
     * @param uid uid
     * @param dst dst
     */
    public static void quickWrite(int uid, ByteBuf dst) {
        dst.writeInt(uid);
    }

    /**
     * 快速读 HeartValue中的uid，这样目的是为了避免出现new这个关键字，提升效率，但是会破坏封装
     * @param value
     * @return
     */
    public static int quickReadUid(ByteBuf value) {
        return value.readInt();
    }

}
