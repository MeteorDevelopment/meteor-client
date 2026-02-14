/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc.swarm;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.utils.misc.text.MessageBuilder;
import net.minecraft.block.Block;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class SwarmWorker extends Thread {
    private Socket socket;
    public Block target;

    public SwarmWorker(String ip, int port) {
        try {
            socket = new Socket(ip, port);
        } catch (Exception e) {
            socket = null;
            MessageBuilder.warning("Server not found at %s on port %s.", ip, port).prefix(MeteorClient.translatable("module.swarm")).send();
            e.printStackTrace();
        }

        if (socket != null) start();
    }

    @Override
    public void run() {
        MessageBuilder.info("Connected to Swarm host on at %s on port %s.", getIp(socket.getInetAddress().getHostAddress()), socket.getPort()).prefix(MeteorClient.translatable("module.swarm")).send();

        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());


            while (!isInterrupted()) {
                String read = in.readUTF();

                if (read.startsWith("swarm")) {
                    MessageBuilder.info("Received command: %s", MessageBuilder.highlight(read)).prefix(MeteorClient.translatable("module.swarm")).send();

                    try {
                        Commands.dispatch(read);
                    } catch (Exception e) {
                        MessageBuilder.error("Error fetching command.").prefix(MeteorClient.translatable("module.swarm")).send();
                        e.printStackTrace();
                    }
                }
            }

            in.close();
        } catch (IOException e) {
            MessageBuilder.error("Error in connection to host.").prefix(MeteorClient.translatable("module.swarm")).send();
            e.printStackTrace();
            disconnect();
        }
    }

    public void disconnect() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        PathManagers.get().stop();

        MessageBuilder.info("Disconnected from host.").prefix(MeteorClient.translatable("module.swarm")).send();

        interrupt();
    }

    public void tick() {
        if (target == null) return;

        PathManagers.get().stop();
        PathManagers.get().mine(target);

        target = null;
    }

    public String getConnection() {
        return getIp(socket.getInetAddress().getHostAddress()) + ":" + socket.getPort();
    }

    private String getIp(String ip) {
        return ip.equals("127.0.0.1") ? "localhost" : ip;
    }
}
