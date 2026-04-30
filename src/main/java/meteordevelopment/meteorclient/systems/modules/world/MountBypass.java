/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;

public class MountBypass extends Module {
    private boolean dontCancel;

    public MountBypass() {
        super(Categories.World, "mount-bypass", "Allows you to bypass the IllegalStacks plugin and put chests on entities.");
    }

    @EventHandler
    public void onSendPacket(PacketEvent.Send event) {
        if (dontCancel) {
            dontCancel = false;
            return;
        }

        if (event.packet instanceof ServerboundAttackPacket(int entityId)
            && mc.level.getEntity(entityId) instanceof AbstractChestedHorse) event.cancel();
    }
}
