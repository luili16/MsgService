package com.llx278.msgservice.niotest;

import java.nio.ByteBuffer;

/**
 * TLV is Type, Length, Value
 * <p>
 * T : 2 byte
 * L : 4 byte
 * V : decide by L
 */
public class Message {

    private byte[] msg;

    public Message(byte[] msg) {
        this.msg = msg;
    }


    public byte[] getMessage() {
        return this.msg;
    }
}
