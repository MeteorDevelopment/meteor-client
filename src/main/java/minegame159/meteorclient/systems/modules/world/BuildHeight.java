/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.world;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.packets.PacketEvent;
import minegame159.meteorclient.mixin.BlockHitResultAccessor;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.math.Direction;

public class BuildHeight extends Module {
    public BuildHeight() {
        super(Categories.World, "build-height", "Allows you to interact with objects at the build limit.");
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof PlayerInteractBlockC2SPacket)) return;

        PlayerInteractBlockC2SPacket p = (PlayerInteractBlockC2SPacket) event.packet;
        if (p.getBlockHitResult().getPos().y >= 255 && p.getBlockHitResult().getSide() == Direction.UP) {
            ((BlockHitResultAccessor) p.getBlockHitResult()).setSide(Direction.DOWN);
        }
    }
}
