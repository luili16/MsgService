package com.llx278.msgservice;

import com.llx278.msgservice.niotest.VariableByteArray;
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

        VariableByteArray va = new VariableByteArray(VariableByteArray.KB);

        byte[] data = new byte[50];
        Random random = new Random(11);
        // 填充数据
        random.nextBytes(data);

        ByteBuffer buffer = ByteBuffer.allocate(512);

        for (int i = 0; i < data.length; i++) {

            buffer.put(data[i]);
            if (buffer.hasRemaining()) {
                va.expand(buffer);
            }
        }

        byte[] res = va.getVariableArray();

        Assert.assertArrayEquals(data,res);

    }


}
