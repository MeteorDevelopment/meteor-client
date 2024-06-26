/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.packets;

import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;

public class PlaySoundPacketEvent {

    private static final PlaySoundPacketEvent INSTANCE = new PlaySoundPacketEvent();

    public PlaySoundS2CPacket packet;

    public static PlaySoundPacketEvent get(PlaySoundS2CPacket packet) {
        INSTANCE.packet = packet;
        return INSTANCE;
    }
}
