/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.packets;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;

public class PacketEvent {
    public static class Receive extends Cancellable {
        private static final Receive INSTANCE = new Receive();

        public Packet<?> packet;
        public ClientConnection connection;

        public static Receive get(Packet<?> packet, ClientConnection connection) {
            INSTANCE.setCancelled(false);
            INSTANCE.packet = packet;
            INSTANCE.connection = connection;
            return INSTANCE;
        }
    }

    public static class Send extends Cancellable {
        private static final Send INSTANCE = new Send();

        public Packet<?> packet;
        public ClientConnection connection;

        public static Send get(Packet<?> packet, ClientConnection connection) {
            INSTANCE.setCancelled(false);
            INSTANCE.packet = packet;
            INSTANCE.connection = connection;
            return INSTANCE;
        }
    }

    public static class Sent {
        private static final Sent INSTANCE = new Sent();

        public Packet<?> packet;
        public ClientConnection connection;

        public static Sent get(Packet<?> packet, ClientConnection connection) {
            INSTANCE.packet = packet;
            INSTANCE.connection = connection;
            return INSTANCE;
        }
    }
}
