/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.packets;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;

public class PacketEvent {
    public static class Receive extends Cancellable {
        public Packet<?> packet;
        public Connection connection;

        public Receive(Packet<?> packet, Connection connection) {
            this.setCancelled(false);
            this.packet = packet;
            this.connection = connection;
        }
    }

    public static class Send extends Cancellable {
        public Packet<?> packet;
        public Connection connection;

        public Send(Packet<?> packet, Connection connection) {
            this.setCancelled(false);
            this.packet = packet;
            this.connection = connection;
        }

        /**
         * Sends a packet without triggering an event. Use when you want to send a packet inside the event
         * listener, without causing a StackOverflowError
         *
         * @param packet The packet to silently send
         */
        public void sendSilently(Packet<?> packet) {
            connection.send(packet, null, true);
        }
    }

    public static class Sent {
        public Packet<?> packet;
        public Connection connection;

        public Sent(Packet<?> packet, Connection connection) {
            this.packet = packet;
            this.connection = connection;
        }

        /**
         * Sends a packet without triggering an event. Use when you want to send a packet inside the event
         * listener, without causing a StackOverflowError
         *
         * @param packet The packet to silently send
         */
        public void sendSilently(Packet<?> packet) {
            connection.send(packet, null, true);
        }
    }
}
