package com.llx278.msgservice.niotest;

import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;

public class Register implements Runnable {

    private static final int MAX_COUNT = 2048;

    private Selector mSelector;

    private LinkedBlockingQueue<SocketChannel> mChannelQueue = new LinkedBlockingQueue<>(MAX_COUNT);

    public Register(Selector selector) {
        mSelector = selector;
        new Thread(this).start();
    }

    public void enqueue(SocketChannel channel) {
        mChannelQueue.add(channel);
    }

    @Override
    public void run() {

    }
}
