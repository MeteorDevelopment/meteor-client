/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerPosition;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

public class NoRotate extends Module {
    public NoRotate() {
        super(Categories.Player, "no-rotate", "Attempts to block rotations sent from server to client.");
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket packet) {
            PlayerPosition oldPosition = packet.change();
            PlayerPosition newPosition = new PlayerPosition(
                oldPosition.position(),
                oldPosition.deltaMovement(),
                mc.player.getYaw(),
                mc.player.getPitch()
            );
            event.packet = PlayerPositionLookS2CPacket.of(
                packet.teleportId(),
                newPosition,
                packet.relatives()
            );
        }
    }
}
