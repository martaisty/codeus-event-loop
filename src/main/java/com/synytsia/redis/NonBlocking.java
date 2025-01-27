package com.synytsia.redis;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;

public class NonBlocking {

    /*
    TODO
    1. Define connection with byte buffer
    2. Store map with key -> connection
    3. Define server to handle clients accept/read/write
    4. Handle client errors and close unused
    5. Write non-blocking test client
     */

    public static void main(String[] args) throws IOException {
        try (final var selector = Selector.open();
             final var serverChannel = ServerSocketChannel.open()) {
            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress("localhost", 23456));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Started at: " + serverChannel.getLocalAddress());
            final var generateResponse = new HashMap<SelectionKey, Boolean>();

            while (true) {
                final int readyCount = selector.select();
                if (readyCount == 0) {
                    continue;
                }

                final var keysIterator = selector.selectedKeys().iterator();
                while (keysIterator.hasNext()) {
                    final var key = keysIterator.next();
                    keysIterator.remove();
                    // TODO check is valid
                    if (key.isAcceptable()) {
                        final var server = ((ServerSocketChannel) key.channel());
                        final var client = server.accept();
                        client.configureBlocking(false);

                        client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                    } else if (key.isReadable()) {
                        final var client = ((SocketChannel) key.channel());

                        final var buff = ByteBuffer.allocate(1024);
                        final var len = client.read(buff);
                        generateResponse.put(key, true);
                        buff.rewind();

                        System.out.println("Client: " + client.getRemoteAddress() + ", read: " + new String(buff.array()));
                    } else if (key.isWritable() && generateResponse.getOrDefault(key, false)) {
                        final var client = ((SocketChannel) key.channel());
                        final var output = ByteBuffer.allocate(25);
                        final var msg = "Hallo";
                        output.putInt(msg.length());
                        output.put(msg.getBytes());
                        output.flip();

                        generateResponse.put(key, false);

                        client.write(output);

                    }
                }
            }


        }
    }
}
