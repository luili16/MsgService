package com.llx278.msgservice.niotest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessageService {

    private int port;

    private Register mRegister;

    private ExecutorService es = Executors.newFixedThreadPool(20);

    //private Map<SelectionKey, SocketWrapper> keysMap = new HashMap<>(100);

    public MessageService(int port) {
        this.port = port;
    }

    public void run() throws IOException {

        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.
                bind(new InetSocketAddress(port)).
                configureBlocking(false);
        Selector selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        mRegister = new Register(selector);

        while (true) {
            int select = selector.select();
            if (select == 0) {
                continue;
            }

            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    SocketChannel sc = serverSocketChannel.accept();
                    sc.configureBlocking(false);
                    sc.register(selector,SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                    SocketWrapper wrapper = new SocketWrapper(sc, key, selector);
                    key.attach(wrapper);
                } else if (key.isReadable()) {

                    SocketWrapper wrapper = (SocketWrapper) key.attachment();


                } else if (key.isWritable()) {


                }
                iterator.remove();
            }
        }
    }

    public static void main(String[] args) {

        MessageService msgService = new MessageService(12306);
        try {
            msgService.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
