/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class SilentMine extends Module {
    public SilentMine() {
        super(Categories.Player, "silent-mine", "Prevents block breaking animations from being visible to other players.");
    }

    private boolean mining;
    private BlockPos lastBlockPos;
    private Direction lastDirection;

    @Override
    public void onDeactivate() {
        mining = false;
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof PlayerActionC2SPacket)) return;

        PlayerActionC2SPacket packet = (PlayerActionC2SPacket) event.packet;

        if (packet.getAction() == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) {
            mining = true;
            lastBlockPos = packet.getPos();
            lastDirection = packet.getDirection();
        }

        if (packet.getAction() == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) {
            mining = false;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mining) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, lastBlockPos, lastDirection));
        }
    }
}
