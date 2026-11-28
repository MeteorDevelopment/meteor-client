/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.player;

import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.world.InteractionHand;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public enum SwingMode {
    Both,
    Packet,
    Client,
    None;

    public static SwingMode of(boolean client) {
        return client ? Client : Packet;
    }

    public boolean client() {
        return this == Client || this == Both;
    }

    private boolean server() {
        return this == Packet || this == Both;
    }

    public void attackSwing(InteractionHand hand) {
        if (client()) mc.player.swing(hand, mc.player.getItemInHand(hand).getAttackAnimation(), false);
        if (server()) mc.getConnection().send(ServerboundPunchPacket.INSTANCE);
    }

    /**
     * Interaction swings are only predicted by the client, the server broadcasts them from its own
     * interaction result. There is no packet to send, so {@link #Packet} and {@link #None} behave alike.
     */
    public void interactSwing(InteractionHand hand) {
        if (client()) mc.player.swing(hand, mc.player.getItemInHand(hand).getInteractAnimation(), false);
    }
}
