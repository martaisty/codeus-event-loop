package com.synytsia.eventloop;

import lombok.Data;

import java.nio.ByteBuffer;

@Data
public class ClientConnection {

    private boolean wantWrite;

    private final ByteBuffer incoming;
    private final ByteBuffer outgoing;

    public ClientConnection() {
        this.wantWrite = false;
        this.incoming = ByteBuffer.allocate(Constants.BUFFER_SIZE);
        this.outgoing = ByteBuffer.allocate(Constants.BUFFER_SIZE);
    }
}
