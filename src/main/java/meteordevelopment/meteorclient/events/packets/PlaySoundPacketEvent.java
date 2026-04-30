/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.packets;

import net.minecraft.network.protocol.game.ClientboundSoundPacket;

public class PlaySoundPacketEvent {

    private static final PlaySoundPacketEvent INSTANCE = new PlaySoundPacketEvent();

    public ClientboundSoundPacket packet;

    public static PlaySoundPacketEvent get(ClientboundSoundPacket packet) {
        INSTANCE.packet = packet;
        return INSTANCE;
    }
}
