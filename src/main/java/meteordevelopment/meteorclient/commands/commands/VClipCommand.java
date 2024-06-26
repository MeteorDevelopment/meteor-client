/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;

public class VClipCommand extends Command {
    public VClipCommand() {
        super("vclip", "Lets you clip through blocks vertically.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("blocks", DoubleArgumentType.doubleArg()).executes(context -> {

            double blocks = context.getArgument("blocks", Double.class);

            // Implementation of "PaperClip" aka "TPX" aka "VaultClip" into vclip
            // Allows you to teleport up to 200 blocks in one go (as you can send 20 move packets per tick)
            // Paper allows you to teleport 10 blocks for each move packet you send in that tick
            // Video explanation by LiveOverflow: https://www.youtube.com/watch?v=3HSnDsfkJT8
            int packetsRequired = (int) Math.ceil(Math.abs(blocks / 10));

            if (packetsRequired > 20) {
                // Wouldn't work on paper anyway.
                // Some servers don't have a vertical limit, so if it is more than 200 blocks, just use a "normal" tp
                // This makes it, so you don't get kicked for sending too many packets
                packetsRequired = 1;
            }

            if (mc.player.hasVehicle()) {
                // Vehicle version
                // For each 10 blocks, send a vehicle move packet with no delta
                for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
                    mc.player.networkHandler.sendPacket(new VehicleMoveC2SPacket(mc.player.getVehicle()));
                }
                // Now send the final vehicle move packet
                mc.player.getVehicle().setPosition(mc.player.getVehicle().getX(), mc.player.getVehicle().getY() + blocks, mc.player.getVehicle().getZ());
                mc.player.networkHandler.sendPacket(new VehicleMoveC2SPacket(mc.player.getVehicle()));
            } else {
                // No vehicle version
                // For each 10 blocks, send a player move packet with no delta
                for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
                }
                // Now send the final player move packet
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + blocks, mc.player.getZ(), true));
                mc.player.setPosition(mc.player.getX(), mc.player.getY() + blocks, mc.player.getZ());
            }

            return SINGLE_SUCCESS;
        }));
    }
}
