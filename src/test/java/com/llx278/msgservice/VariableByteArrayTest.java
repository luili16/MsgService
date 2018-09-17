package com.llx278.msgservice;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.ByteBuffer;
import java.util.Random;

@RunWith(JUnit4.class)
public class VariableByteArrayTest {

    @Before
    public void before() {

    }

    @After
    public void after() {

    }

    @Test
    public void test() {

        ByteBuf buf = Unpooled.buffer();


        ByteBuf buf2 = Unpooled.buffer();

        ByteBuf buf3 = Unpooled.buffer();

        CompositeByteBuf com = Unpooled.compositeBuffer();

        ((CompositeByteBuf) com).addComponent(true,buf);
        ((CompositeByteBuf) com).addComponent(true,buf2);
        ((CompositeByteBuf) com).addComponent(true,buf3);

        System.out.println("com : " + com.refCnt());
        System.out.println("buf count : " + buf.refCnt());
        System.out.println("buf count1 : " + buf2.refCnt());
        System.out.println("buf count3 : " + buf3.refCnt());

        ((ByteBuf)com).release();

        System.out.println("com : " + com.refCnt());
        System.out.println("buf count : " + buf.refCnt());
        System.out.println("buf count1 : " + buf2.refCnt());
        System.out.println("buf count3 : " + buf3.refCnt());

    }


}
