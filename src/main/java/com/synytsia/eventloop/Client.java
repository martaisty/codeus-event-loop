package com.synytsia.eventloop;

import javax.net.SocketFactory;
import java.io.*;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) throws IOException {
        try (final var socket = SocketFactory.getDefault().createSocket("localhost", 25678);
             final var is = new DataInputStream(socket.getInputStream());
             final var os = new DataOutputStream(socket.getOutputStream())) {

            final var scanner = new Scanner(System.in);
            while (true) {
                final var cliInput = scanner.nextLine();
                if (cliInput.equals("exit")) {
                    break;
                }

                os.writeInt(cliInput.length());
                os.writeBytes(cliInput);
                os.flush();

                final var len = is.readInt();
                final var msg = new String(is.readNBytes(len));
                System.out.println("Server len: " + len);
                System.out.println("Server msg: " + msg);
            }
        }
    }
}
