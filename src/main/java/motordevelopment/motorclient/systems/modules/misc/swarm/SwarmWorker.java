/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.systems.modules.misc.swarm;

import motordevelopment.motorclient.commands.Commands;
import motordevelopment.motorclient.pathing.PathManagers;
import motordevelopment.motorclient.utils.player.ChatUtils;
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
            ChatUtils.warningPrefix("Swarm", "Server not found at %s on port %s.", ip, port);
            e.printStackTrace();
        }

        if (socket != null) start();
    }

    @Override
    public void run() {
        ChatUtils.infoPrefix("Swarm", "Connected to Swarm host on at %s on port %s.", getIp(socket.getInetAddress().getHostAddress()), socket.getPort());

        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());


            while (!isInterrupted()) {
                String read = in.readUTF();

                if (read.startsWith("swarm")) {
                    ChatUtils.infoPrefix("Swarm", "Received command: (highlight)%s", read);

                    try {
                        Commands.dispatch(read);
                    } catch (Exception e) {
                        ChatUtils.error("Error fetching command.");
                        e.printStackTrace();
                    }
                }
            }

            in.close();
        } catch (IOException e) {
            ChatUtils.errorPrefix("Swarm", "Error in connection to host.");
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

        ChatUtils.infoPrefix("Swarm", "Disconnected from host.");

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
