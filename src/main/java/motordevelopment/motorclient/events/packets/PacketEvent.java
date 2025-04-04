/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.events.packets;

import motordevelopment.motorclient.events.Cancellable;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;

public class PacketEvent {
    public static class Receive extends Cancellable {
        public Packet<?> packet;
        public ClientConnection connection;

        public Receive(Packet<?> packet, ClientConnection connection) {
            this.setCancelled(false);
            this.packet = packet;
            this.connection = connection;
        }
    }

    public static class Send extends Cancellable {
        public Packet<?> packet;
        public ClientConnection connection;

        public Send(Packet<?> packet, ClientConnection connection) {
            this.setCancelled(false);
            this.packet = packet;
            this.connection = connection;
        }
    }

    public static class Sent {
        public Packet<?> packet;
        public ClientConnection connection;

        public Sent(Packet<?> packet, ClientConnection connection) {
            this.packet = packet;
            this.connection = connection;
        }
    }
}
