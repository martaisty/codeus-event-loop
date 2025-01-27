package com.synytsia.redis.eventloop;

import java.nio.ByteBuffer;

public class Main {

    public static void main(String[] args) {
        final var eventLoop = new EventLoop();

        eventLoop.start();
    }
}
