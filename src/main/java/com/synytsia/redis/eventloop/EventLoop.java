package com.synytsia.redis.eventloop;

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

    private final Map<SelectionKey, ClientConnection> clients = new HashMap<>();

    public void start() {
        log.info("Starting event loop");

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
                    keysIterator.remove();

                    if (key.isAcceptable()) {
                        acceptClient(selector, (ServerSocketChannel) key.channel());
                    } else if (!key.isValid() || clients.get(key).isWantClose()) {
                        closeClient(key);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    } else if (key.isWritable()) {
                        handleWrite(key);
                    }
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private void acceptClient(final Selector selector, final ServerSocketChannel server) throws IOException {
        final var client = server.accept();
        log.debug("New client connected: {}", client.getRemoteAddress());

        client.configureBlocking(false);

        final var clientKey = client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        clients.put(clientKey, new ClientConnection());
    }

    private void handleRead(final SelectionKey key) throws IOException {
        final var client = (SocketChannel) key.channel();
        final var connection = clients.get(key);

        final var readSize = client.read(connection.getIncoming());
        if (readSize < 0) {
            log.debug("EOF for client: {}, closing connection", client.getRemoteAddress());
            connection.setWantClose(true);
            return;
        }
        log.debug("Read {} bytes from client {}", readSize, client.getRemoteAddress());

        parseRequest(connection);
    }

    private void parseRequest(final ClientConnection connection) {
        final var incoming = connection.getIncoming();

        if (incoming.position() < Constants.MESSAGE_LENGTH_SIZE) {
            log.trace("Didn't receive full data. Received data length: {}", incoming.position());
            return;
        }

        final int msgSize = incoming.getInt(0);
        if (msgSize > Constants.MAX_MESSAGE_SIZE) {
            log.error("Message size exceeds limit, closing connection. Maximum allowed: {}, specified in request: {}", Constants.MAX_MESSAGE_SIZE, msgSize);
            connection.setWantClose(true);
            return;

        }
        if (incoming.position() - Constants.MESSAGE_LENGTH_SIZE < msgSize) {
            log.trace("Didn't receive full data. Received data length: {}, specified in request: {}", incoming.position(), msgSize);
            return;
        }

        incoming.flip();
        final var message = new String(incoming.array(), Constants.MESSAGE_LENGTH_SIZE, msgSize);
        incoming.clear();


        log.debug("Received data: '{}'", message);

        // TODO application logic
        writeResponse(connection, message);
    }

    private void writeResponse(final ClientConnection connection, final String message) {
        log.trace("Generating echo response");
        final var outgoing = connection.getOutgoing();
        outgoing.putInt(message.length())
                .put(message.getBytes());
        outgoing.flip();
        connection.setWantWrite(true);
    }

    private void handleWrite(final SelectionKey key) throws IOException {
        final var connection = clients.get(key);
        if (!connection.isWantWrite()) {
            return;
        }
        final var client = (SocketChannel) key.channel();

        final var written = client.write(connection.getOutgoing());
        log.trace("Wrote {} bytes to client {}", written, client.getRemoteAddress());

        if (!connection.getOutgoing().hasRemaining()) {
            connection.setWantWrite(false);
            connection.getOutgoing().clear();
        }
    }

    private void closeClient(final SelectionKey key) {
        try (final var client = (SocketChannel) key.channel()) {
            log.debug("Closing client: {}", client.getRemoteAddress());
        } catch (IOException e) {
            log.error("Failed to close client");
        } finally {
            log.info("Closed client");
        }
    }

}
