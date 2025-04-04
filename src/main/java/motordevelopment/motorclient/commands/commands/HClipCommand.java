/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.commands.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import motordevelopment.motorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class HClipCommand extends Command {
    public HClipCommand() {
        super("hclip", "Lets you clip through blocks horizontally.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("blocks", DoubleArgumentType.doubleArg()).executes(context -> {
            double blocks = context.getArgument("blocks", Double.class);
            Vec3d forward = Vec3d.fromPolar(0, mc.player.getYaw()).normalize();

            if (mc.player.hasVehicle()) {
                Entity vehicle = mc.player.getVehicle();
                vehicle.setPosition(vehicle.getX() + forward.x * blocks, vehicle.getY(), vehicle.getZ() + forward.z * blocks);
            }

            mc.player.setPosition(mc.player.getX() + forward.x * blocks, mc.player.getY(), mc.player.getZ() + forward.z * blocks);

            return SINGLE_SUCCESS;
        }));
    }
}
