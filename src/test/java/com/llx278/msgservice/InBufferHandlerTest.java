package com.llx278.msgservice;

import com.llx278.msgservice.protocol.TLV;
import com.llx278.msgservice.protocol.Type;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.CharsetUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Random;

import static org.junit.Assert.assertEquals;
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

        EmbeddedChannel channel = new EmbeddedChannel(new InBufferHandler());
        assertTrue(channel.writeInbound(buf));
        assertTrue(channel.finish());

        ByteBuf o = channel.readInbound();
        assertEquals(1234,o.getInt(8));
    }

    @Test
    public void oneSyncMsgHuge() {

        ByteBuf buf1 = Unpooled.buffer();
        buf1.writeBytes(TLV.SYNC_BYTES);
        buf1.writeInt(Type.FRAME_MSG);

        ByteBuf buf2 = Unpooled.buffer();
        String str = getRandomString(50000);
        buf2.writeCharSequence(str,CharsetUtil.UTF_8);
        int len = buf2.readableBytes();
        buf1.writeInt(len);
        buf2.writeBytes(TLV.FINISH_BYTES);

        CompositeByteBuf cb = Unpooled.compositeBuffer();
        cb.addComponents(true,buf1,buf2);

        InBufferHandler h = new InBufferHandler();
        EmbeddedChannel channel1 = new EmbeddedChannel(h);
        assertTrue(channel1.writeInbound(cb));
        assertTrue(channel1.finish());

        ByteBuf resBuf = channel1.readInbound();
        assertEquals(len,resBuf.getInt(4));
    }

    public static String getRandomString(int length) {
        //定义一个字符串（A-Z，a-z，0-9）即62位；
        String str = "zxcvbnmlkjhgfdsaqwertyuiopQWERTYUIOPASDFGHJKLZXCVBNM1234567890";
        //由Random生成随机数
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        //长度为几就循环几次
        for (int i = 0; i < length; ++i) {
            //产生0-61的数字
            int number = random.nextInt(62);
            //将产生的数字通过length次承载到sb中
            sb.append(str.charAt(number));
        }
        //将承载的字符转换成字符串
        return sb.toString();
    }
}
