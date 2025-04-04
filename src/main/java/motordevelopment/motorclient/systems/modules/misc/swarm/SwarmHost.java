/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.systems.modules.misc.swarm;

import motordevelopment.motorclient.utils.network.MotorExecutor;
import motordevelopment.motorclient.utils.player.ChatUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SwarmHost extends Thread {
    private ServerSocket socket;
    private final SwarmConnection[] clientConnections = new SwarmConnection[50];

    public SwarmHost(int port) {
        try {
            socket = new ServerSocket(port);
        } catch (IOException e) {
            socket = null;
            ChatUtils.errorPrefix("Swarm", "Couldn't start a server on port %s.", port);
            e.printStackTrace();
        }

        if (socket != null) start();
    }

    @Override
    public void run() {
        ChatUtils.infoPrefix("Swarm", "Listening for incoming connections on port %s.", socket.getLocalPort());

        while (!isInterrupted()) {
            try {
                Socket connection = socket.accept();
                assignConnectionToSubServer(connection);
            } catch (IOException e) {
                ChatUtils.errorPrefix("Swarm", "Error making a connection to worker.");
                e.printStackTrace();
            }
        }
    }

    public void assignConnectionToSubServer(Socket connection) {
        for (int i = 0; i < clientConnections.length; i++) {
            if (this.clientConnections[i] == null) {
                this.clientConnections[i] = new SwarmConnection(connection);
                break;
            }
        }
    }

    public void disconnect() {
        for (SwarmConnection connection : clientConnections) {
            if (connection != null) connection.disconnect();
        }

        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ChatUtils.infoPrefix("Swarm", "Server closed on port %s.", socket.getLocalPort());

        interrupt();
    }

    public void sendMessage(String s) {
        MotorExecutor.execute(() -> {
            for (SwarmConnection connection : clientConnections) {
                if (connection != null) {
                    connection.messageToSend = s;
                }
            }
        });
    }

    public SwarmConnection[] getConnections() {
        return clientConnections;
    }

    public int getConnectionCount() {
        int count = 0;

        for (SwarmConnection clientConnection : clientConnections) {
            if (clientConnection != null) count++;
        }

        return count;
    }
}
