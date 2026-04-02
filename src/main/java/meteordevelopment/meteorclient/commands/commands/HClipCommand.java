/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class HClipCommand extends Command {
    public HClipCommand() {
        super("hclip", "Lets you clip through blocks horizontally.");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.then(argument("blocks", DoubleArgumentType.doubleArg()).executes(context -> {
            double blocks = context.getArgument("blocks", Double.class);
            Vec3 forward = Vec3.directionFromRotation(0, mc.player.getYRot()).normalize();

            if (mc.player.isPassenger()) {
                Entity vehicle = mc.player.getVehicle();
                vehicle.setPos(vehicle.getX() + forward.x * blocks, vehicle.getY(), vehicle.getZ() + forward.z * blocks);
            }

            mc.player.setPos(mc.player.getX() + forward.x * blocks, mc.player.getY(), mc.player.getZ() + forward.z * blocks);

            return SINGLE_SUCCESS;
        }));
    }
}
