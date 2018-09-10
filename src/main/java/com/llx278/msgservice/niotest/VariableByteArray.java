package com.llx278.msgservice.niotest;

import java.nio.ByteBuffer;


public class VariableByteArray {

    public static int KB = 1024;

    private byte[] mVariableArray;

    private int mOffset = 0;
    private int mCapacity;

    public VariableByteArray(int capacity) {
        this.mCapacity = capacity;
        mVariableArray = new byte[capacity];
    }

    public void expand(ByteBuffer buffer) {
        buffer.flip();
        while (buffer.hasRemaining()) {
            if (mOffset == mVariableArray.length) {
                byte[] temp = mVariableArray;
                mCapacity = mCapacity * 2;
                mVariableArray = new byte[mCapacity];
                System.arraycopy(temp,0,mVariableArray,0,temp.length);
            }
            mVariableArray[mOffset] = buffer.get();
            mOffset++;
        }
        buffer.clear();
    }

    public byte[] getVariableArray() {
        byte[] data = new byte[mOffset];
        System.arraycopy(mVariableArray,0,data,0, mOffset);
        return data;

    }
}

