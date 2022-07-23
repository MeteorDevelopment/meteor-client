/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands.commands;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.systems.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.MathHelper;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class RotationCommand extends Command {
    public RotationCommand() {
        super("rotation", "Modifies your rotation.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder
            .then(literal("set")
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
