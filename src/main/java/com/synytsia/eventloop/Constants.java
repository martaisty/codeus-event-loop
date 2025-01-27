package com.synytsia.eventloop;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Constants {
    /**
     * port that server is listening to
     */
    public static final int PORT = 25678;

    public static final int KB = 1024;
    public static final int MB = 1024 * KB;

    public static final int MESSAGE_LENGTH_SIZE = Integer.BYTES;
    public static final int MAX_MESSAGE_SIZE = 16 * MB;
    /**
     * int length + max message size;  requests: [n|msg][n|msg][n|msg]...
     */
    public static final int BUFFER_SIZE = MESSAGE_LENGTH_SIZE + MAX_MESSAGE_SIZE;
}
