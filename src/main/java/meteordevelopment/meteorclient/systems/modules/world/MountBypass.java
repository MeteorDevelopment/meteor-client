/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;

public class MountBypass extends Module {
    private boolean dontCancel;

    public MountBypass() {
        super(Categories.World, "mount-bypass", "Allows you to bypass the IllegalStacks plugin and put chests on entities.");
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {

        if (dontCancel) {
            dontCancel = false;
            return;
        }

        if (!(event.packet instanceof PlayerInteractEntityC2SPacket)) return;
        PlayerInteractEntityC2SPacket packet = (PlayerInteractEntityC2SPacket) event.packet;

        // TODO: Fix
        /*if (packet.getType() == PlayerInteractEntityC2SPacket.InteractionType.INTERACT_AT && packet.getEntity(mc.world) instanceof AbstractDonkeyEntity) {
            event.cancel();
        }*/
    }
}
