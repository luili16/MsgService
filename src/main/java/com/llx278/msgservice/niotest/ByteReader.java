package com.llx278.msgservice.niotest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ByteReader {

    public Message readFrom(SocketChannel channel) throws IOException,UnExceptedEOS {

        VariableByteArray byteArray = new VariableByteArray(VariableByteArray.KB);

        ByteBuffer buffer = ByteBuffer.allocate(VariableByteArray.KB);
        int count ;
        while ((count = channel.read(buffer)) > 0) {
            byteArray.expand(buffer);
        }

        if (count == -1) {
            throw new UnExceptedEOS("read data failed");
        }

        Message msg = new Message(byteArray.getVariableArray());

        System.out.println("读取结束");

        return msg;
    }

}
