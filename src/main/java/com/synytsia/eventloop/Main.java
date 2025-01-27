package com.synytsia.eventloop;

public class Main {

    public static void main(String[] args) {
        final var eventLoop = new EventLoop();

        eventLoop.start();
    }
}
