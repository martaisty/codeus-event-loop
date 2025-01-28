package com.synytsia.eventloop;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventLoopTest {

    private final ExecutorService eventLoopExecutor = Executors.newSingleThreadExecutor();

    @BeforeEach
    void setUp() {
        final var evenLoop = new EventLoop();

        eventLoopExecutor.submit(evenLoop::start);
    }

    @AfterEach
    void tearDown() {
        eventLoopExecutor.shutdownNow();
    }

    @Test
    void e2eTest() throws IOException, InterruptedException {
        final var address = new InetSocketAddress("localhost", Constants.PORT);
        // Wait until server starts
        Thread.sleep(1_000);
        try (final var client = SocketChannel.open(address)) {


            final var buffer = ByteBuffer.allocate(256);
            final var message = "Hello";
            buffer.putInt(message.length())
                    .put(message.getBytes())
                    .flip();

            final var written = client.write(buffer);
            assertEquals(4 + message.length(), written);
            buffer.clear();

            final var read = client.read(buffer);
            assertEquals(4 + message.length(), read);

            buffer.flip();

            final var messageLength = buffer.getInt();
            assertEquals(5, messageLength);
            final var readMsg = new String(buffer.array(), 4, messageLength);
            assertEquals("Hello", readMsg);
        }
    }
}