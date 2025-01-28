package com.synytsia.eventloop;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
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
        log.debug("Starting event loop on {}", Constants.PORT);
        try (final var selector = Selector.open();
             final var server = ServerSocketChannel.open()) {

            server.configureBlocking(false);
            server.bind(new InetSocketAddress(Constants.PORT));
            server.register(selector, SelectionKey.OP_ACCEPT);
            log.info("Started event loop on {}", server.getLocalAddress());

            while (true) {
                final int ready = selector.select();
                if (ready == 0) {
                    continue;
                }

                final var keysIterator = selector.selectedKeys().iterator();
                while (keysIterator.hasNext()) {
                    final var key = keysIterator.next();

                    if (key.isAcceptable()) {
                        acceptClient(selector, (ServerSocketChannel) key.channel());
                    } else if (key.isReadable()) {
                        handleRead(key);
                    } else if (key.isWritable()) {
                        handleWrite(key);
                    }

                    keysIterator.remove();
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void acceptClient(final Selector selector, final ServerSocketChannel server) throws IOException {
        final var client = server.accept();
        client.configureBlocking(false);
        log.info("New client connected: {}", client.getRemoteAddress());

        final var clientKey = client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        clients.put(clientKey, new ClientConnection());
    }

    private void handleRead(final SelectionKey key) throws IOException {
        final var client = (SocketChannel) key.channel();
        final var connection = clients.get(key);

        final var readSize = client.read(connection.getIncoming());
        if (readSize < 0) {
            log.debug("EOF for client {}", client.getRemoteAddress());
            closeClient(key);
            return;
        }
        log.trace("Read {} bytes from client: {}", readSize, client.getRemoteAddress());

        parseRequest(connection);
    }

    private void handleWrite(final SelectionKey key) throws IOException {
        final var connection = clients.get(key);
        if (!connection.isWantWrite()) {
            return;
        }

        final var client = (SocketChannel) key.channel();
        final var outgoing = connection.getOutgoing();
        final int written = client.write(outgoing);
        log.trace("Wrote {} bytes to client: {}. Remaining: {}", written, client.getRemoteAddress(), outgoing.remaining());

        if (!outgoing.hasRemaining()) {
            connection.setWantWrite(false);
            outgoing.clear();
        }
    }

    private void parseRequest(final ClientConnection connection) {
        final var incoming = connection.getIncoming();

        if (incoming.position() < Constants.MESSAGE_LENGTH_SIZE) {
            log.trace("Didn't receive full data yet. Received {} bytes.", incoming.position());
            return;
        }

        final var msgSize = incoming.getInt(0);
        if (incoming.position() - Constants.MESSAGE_LENGTH_SIZE < msgSize) {
            log.trace("Didn't receive full message yet. Received {} bytes of message, expected: {}", incoming.position() - Constants.MESSAGE_LENGTH_SIZE, msgSize);
            return;
        }

        incoming.flip();
        final var message = new String(incoming.array(), Constants.MESSAGE_LENGTH_SIZE, msgSize);
        log.info("Client request: '{}'", message);
        incoming.clear();

        writeResponse(connection, message);
    }

    private void writeResponse(final ClientConnection connection, final String message) {
        log.trace("Writing echo response");
        connection.getOutgoing()
                .putInt(message.length())
                .put(message.getBytes())
                .flip();
        connection.setWantWrite(true);
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