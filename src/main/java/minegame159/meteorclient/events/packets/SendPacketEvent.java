/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.events.packets;

import minegame159.meteorclient.events.Cancellable;
import net.minecraft.network.Packet;

public class SendPacketEvent extends Cancellable {
    public Packet<?> packet;
}
