package com.synytsia.redis.eventloop;

import lombok.Data;

import java.nio.ByteBuffer;

@Data
public class ClientConnection {

    private boolean wantWrite;
    private boolean wantClose;

    private final ByteBuffer incoming;
    private final ByteBuffer outgoing;

    public ClientConnection() {
        this.wantWrite = false;
        this.wantClose = false;
        this.incoming = ByteBuffer.allocate(Constants.BUFFER_SIZE);
        this.outgoing = ByteBuffer.allocate(Constants.BUFFER_SIZE);
    }
}
