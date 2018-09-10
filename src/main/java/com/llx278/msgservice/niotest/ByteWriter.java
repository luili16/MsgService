package com.llx278.msgservice.niotest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ByteWriter {


    public void writeTo(SocketChannel channel, Message message) throws IOException, UnExceptedEOS {

        byte[] bytes = message.getMessage();

        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        int count;
        while ((count = channel.write(buffer)) > 0) {
            if (!buffer.hasRemaining()) {
                break;
            }
        }

        if (count == -1) {
            throw new UnExceptedEOS("write data failed");
        }
        System.out.println("写入结束");
    }
}
