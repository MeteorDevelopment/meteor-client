/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc.swarm;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.misc.text.MessageBuilder;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SwarmConnection extends Thread {
    public final Socket socket;
    public String messageToSend;

    public SwarmConnection(Socket socket) {
        this.socket = socket;
        start();
    }

    @Override
    public void run() {
        MessageBuilder.info("New worker connected on %s.", getIp(socket.getInetAddress().getHostAddress())).prefix(MeteorClient.translatable("module.swarm")).send();

        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            while (!isInterrupted()) {
                if (messageToSend != null) {
                    try {
                        out.writeUTF(messageToSend);
                        out.flush();
                    } catch (Exception e) {
                        MessageBuilder.error("Encountered error when sending command.").prefix(MeteorClient.translatable("module.swarm")).send();
                        e.printStackTrace();
                    }

                    messageToSend = null;
                }
            }

            out.close();
        } catch (IOException e) {
            MessageBuilder.info("Error creating a connection with %s on port %s.", getIp(socket.getInetAddress().getHostAddress()), socket.getPort()).prefix(MeteorClient.translatable("module.swarm")).send();
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        MessageBuilder.info("Worker disconnected on ip: %s.", socket.getInetAddress().getHostAddress()).prefix(MeteorClient.translatable("module.swarm")).send();

        interrupt();
    }

    public String getConnection() {
        return getIp(socket.getInetAddress().getHostAddress()) + ":" + socket.getPort();
    }

    private String getIp(String ip) {
        return ip.equals("127.0.0.1") ? "localhost" : ip;
    }
}
