package com.synytsia.eventloop.eventloop;

public class Main {

    public static void main(String[] args) {
        final var eventLoop = new EventLoop();

        eventLoop.start();
    }
}
