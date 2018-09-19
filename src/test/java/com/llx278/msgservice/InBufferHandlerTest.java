package com.llx278.msgservice;

import com.llx278.msgservice.protocol.TLV;
import com.llx278.msgservice.protocol.Type;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.LinkedList;

import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class InBufferHandlerTest {

    @Test
    public void oneSyncMsg() {

        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(TLV.SYNC_BYTES);
        buf.writeInt(Type.FRAME_HEART);
        buf.writeInt(4);
        buf.writeInt(1234);
        buf.writeBytes(TLV.FINISH_BYTES);


        System.out.println("buf is : " + buf.readableBytes());
        EmbeddedChannel channel = new EmbeddedChannel(new InBufferHandler());
        assertTrue(channel.writeInbound(buf));
        assertTrue(channel.finish());

        ByteBuf o = channel.readInbound();
        System.out.println("o is : " + o.getInt(8));
        ByteBuf o1 = channel.readInbound();
        System.out.println("o is : " + o1.getInt(8));

    }

}
