package com.synytsia.redis;

import javax.net.ServerSocketFactory;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        try (final var server = ServerSocketFactory.getDefault().createServerSocket(23456)) {

            
            while (true) {
                try (final var client = server.accept();
                     final var is = new DataInputStream(client.getInputStream());
                     final var os = new DataOutputStream(client.getOutputStream())) {
                    System.out.println("Connection accepted");

                    // 1 client connection at once
                    while (true) {
                        final int len = is.readInt();
                        final var message = new String(is.readNBytes(len));
                        System.out.println("Client len: " + len);
                        System.out.println("Client message: " + message);

                        final var outputMsg = "Accepted";
                        os.writeInt(outputMsg.length());
                        os.writeBytes(outputMsg);
                    }
                } catch (EOFException ex) {
                    System.out.println("Connection closed: " + ex.getMessage());
                }
            }
        }
    }
}
