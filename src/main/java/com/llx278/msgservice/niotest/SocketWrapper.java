package com.llx278.msgservice.niotest;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class SocketWrapper {

    private SocketChannel channel;
    private SelectionKey key;
    private Selector selector;

    public SocketWrapper(SocketChannel channel, SelectionKey key, Selector selector) {
        this.channel = channel;
        this.key = key;
        this.selector = selector;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public SelectionKey getKey() {
        return key;
    }

    public Selector getSelector() {
        return selector;
    }
}
