/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.systems.commands.Command;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class VClipCommand extends Command {
    public VClipCommand() {
        super("vclip", "Lets you clip through blocks vertically.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("blocks", DoubleArgumentType.doubleArg()).executes(context -> {
            ClientPlayerEntity player = mc.player;
            assert player != null;

            double blocks = context.getArgument("blocks", Double.class);

            // Implementation of "PaperClip" aka "TPX" aka "VaultClip" into vclip
            // Allows you to teleport up to 200 blocks in one go (as you can send 20 move packets per tick)
            // Paper allows you to teleport 10 blocks for each move packet you send in that tick
            // Video explanation by LiveOverflow: https://www.youtube.com/watch?v=3HSnDsfkJT8
            int packetsRequired = (int) Math.ceil(blocks / 10);
            if (player.hasVehicle()) {
                // Vehicle version
                // For each 10 blocks, send a vehicle move packet with no delta
                for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
                    player.networkHandler.sendPacket(new VehicleMoveC2SPacket(player.getVehicle()));
                }
                // Now send the final vehicle move packet
                ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(3*8+2*4);
                buf.writeDouble(player.getVehicle().getX());
                buf.writeDouble(player.getVehicle().getY() + blocks);
                buf.writeDouble(player.getVehicle().getZ());
                buf.writeFloat(player.getVehicle().getYaw());
                buf.writeFloat(player.getVehicle().getPitch());
                player.networkHandler.sendPacket(new VehicleMoveC2SPacket(new PacketByteBuf(buf)));
                player.getVehicle().setPosition(player.getX(), player.getY() + blocks, player.getZ());
            } else {
                // No vehicle version
                // For each 10 blocks, send a player move packet with no delta
                for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
                    player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
                }
                // Now send the final player move packet
                player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(player.getX(), player.getY() + blocks, player.getZ(), true));
                player.setPosition(player.getX(), player.getY() + blocks, player.getZ());
            }

            return SINGLE_SUCCESS;
        }));
    }
}
