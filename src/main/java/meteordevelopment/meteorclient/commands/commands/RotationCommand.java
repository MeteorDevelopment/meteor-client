/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.DirectionArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public class RotationCommand extends Command {
    public RotationCommand() {
        super("rotation", "Modifies your rotation.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder
            .then(literal("set")
                .then(argument("direction", DirectionArgumentType.create())
                    .executes(context -> {
                        mc.player.setPitch(context.getArgument("direction", Direction.class).getVector().getY() * -90);
                        mc.player.setYaw(context.getArgument("direction", Direction.class).asRotation());

                        return SINGLE_SUCCESS;
                    }))
                .then(argument("pitch", FloatArgumentType.floatArg(-90, 90))
                    .executes(context -> {
                        mc.player.setPitch(context.getArgument("pitch", Float.class));

                        return SINGLE_SUCCESS;
                    })
                    .then(argument("yaw", FloatArgumentType.floatArg(-180, 180))
                        .executes(context -> {
                            mc.player.setPitch(context.getArgument("pitch", Float.class));
                            mc.player.setYaw(context.getArgument("yaw", Float.class));

                            return SINGLE_SUCCESS;
                        })
                    )
                )
            )
            .then(literal("add")
                .then(argument("pitch", FloatArgumentType.floatArg(-90, 90))
                    .executes(context -> {
                        float pitch = mc.player.getPitch() + context.getArgument("pitch", Float.class);
                        mc.player.setPitch(pitch >= 0 ? Math.min(pitch, 90) : Math.max(pitch, -90));

                        return SINGLE_SUCCESS;
                    })
                    .then(argument("yaw", FloatArgumentType.floatArg(-180, 180))
                        .executes(context -> {
                            float pitch = mc.player.getPitch() + context.getArgument("pitch", Float.class);
                            mc.player.setPitch(pitch >= 0 ? Math.min(pitch, 90) : Math.max(pitch, -90));

                            float yaw = mc.player.getYaw() + context.getArgument("yaw", Float.class);
                            mc.player.setYaw(MathHelper.wrapDegrees(yaw));

                            return SINGLE_SUCCESS;
                        })
                    )
                )
            );
    }
}
