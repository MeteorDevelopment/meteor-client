/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.packets;

import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;

public class InventoryEvent {
    private static final InventoryEvent INSTANCE = new InventoryEvent();

    public ClientboundContainerSetContentPacket packet;

    public static InventoryEvent get(ClientboundContainerSetContentPacket packet) {
        INSTANCE.packet = packet;
        return INSTANCE;
    }
}
