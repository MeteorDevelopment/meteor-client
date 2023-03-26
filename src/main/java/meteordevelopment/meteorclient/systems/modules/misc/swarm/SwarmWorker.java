/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc.swarm;

import baritone.api.BaritoneAPI;
import meteordevelopment.meteorclient.systems.commands.Commands;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
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

                if (!read.equals("")) {
                    ChatUtils.infoPrefix("Swarm", "Received command: (highlight)%s", read);

                    try {
                        Commands.get().dispatch(read);
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

        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();

        ChatUtils.infoPrefix("Swarm", "Disconnected from host.");

        interrupt();
    }

    public void tick() {
        if (target == null) return;
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        BaritoneAPI.getProvider().getPrimaryBaritone().getMineProcess().mine(target);
        target = null;
    }

    public String getConnection() {
        return getIp(socket.getInetAddress().getHostAddress()) + ":" + socket.getPort();
    }

    private String getIp(String ip) {
        return ip.equals("127.0.0.1") ? "localhost" : ip;
    }
}
