package com.synytsia.eventloop;

import com.synytsia.eventloop.exceptions.ExerciseNotCompletedException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class EventLoop {

    /**
     * Opened connections
     */
    private final Map<SelectionKey, ClientConnection> clients = new HashMap<>();

    public void start() {
        throw new ExerciseNotCompletedException();
    }

    private void acceptClient(final Selector selector, final ServerSocketChannel server) throws IOException{
        throw new ExerciseNotCompletedException();
    }

    private void handleRead(final SelectionKey key) throws IOException {
        throw new ExerciseNotCompletedException();
    }

    private void handleWrite(final SelectionKey key) throws IOException {
        throw new ExerciseNotCompletedException();
    }

    private void parseRequest(final ClientConnection connection) {
    }

    private void writeResponse(final ClientConnection connection, final String message) {
    }

    private void closeClient(final SelectionKey key) {
        try (final var clientChannel = (SocketChannel) key.channel()) {
            clients.remove(key);
            log.debug("Closing client: {}", clientChannel.getRemoteAddress());
        } catch (IOException e) {
            log.error("Failed to close client", e);
        } finally {
            log.info("Closed client");
        }
    }
}