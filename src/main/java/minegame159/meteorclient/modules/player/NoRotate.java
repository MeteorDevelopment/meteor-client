/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import minegame159.meteorclient.events.packets.PacketEvent;
import minegame159.meteorclient.mixininterface.IEntitySetHeadYawS2CPacket;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;

public class NoRotate extends Module {

    public NoRotate() {
        super(Category.Player, "no-rotate", "Attempts to block rotations sent from server to client.");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof EntitySetHeadYawS2CPacket) {
            ((IEntitySetHeadYawS2CPacket) event.packet).setPitch(mc.player.getPitch(0));
            ((IEntitySetHeadYawS2CPacket) event.packet).setYaw(mc.player.getYaw(1));
        }
    }
}
