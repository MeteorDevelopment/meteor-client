/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.DirectionArgumentType;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

public class RotationCommand extends Command {
    public RotationCommand() {
        super("rotation", "Modifies your rotation.");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder
            .then(literal("set")
                .then(argument("direction", DirectionArgumentType.create())
                    .executes(context -> {
                        mc.player.setXRot(context.getArgument("direction", Direction.class).getUnitVec3i().getY() * -90);
                        mc.player.setYRot(context.getArgument("direction", Direction.class).toYRot());

                        return SINGLE_SUCCESS;
                    }))
                .then(argument("pitch", FloatArgumentType.floatArg(-90, 90))
                    .executes(context -> {
                        mc.player.setXRot(context.getArgument("pitch", Float.class));

                        return SINGLE_SUCCESS;
                    })
                    .then(argument("yaw", FloatArgumentType.floatArg(-180, 180))
                        .executes(context -> {
                            mc.player.setXRot(context.getArgument("pitch", Float.class));
                            mc.player.setYRot(context.getArgument("yaw", Float.class));

                            return SINGLE_SUCCESS;
                        })
                    )
                )
            )
            .then(literal("add")
                .then(argument("pitch", FloatArgumentType.floatArg(-90, 90))
                    .executes(context -> {
                        float pitch = mc.player.getXRot() + context.getArgument("pitch", Float.class);
                        mc.player.setXRot(pitch >= 0 ? Math.min(pitch, 90) : Math.max(pitch, -90));

                        return SINGLE_SUCCESS;
                    })
                    .then(argument("yaw", FloatArgumentType.floatArg(-180, 180))
                        .executes(context -> {
                            float pitch = mc.player.getXRot() + context.getArgument("pitch", Float.class);
                            mc.player.setXRot(pitch >= 0 ? Math.min(pitch, 90) : Math.max(pitch, -90));

                            float yaw = mc.player.getYRot() + context.getArgument("yaw", Float.class);
                            mc.player.setYRot(Mth.wrapDegrees(yaw));

                            return SINGLE_SUCCESS;
                        })
                    )
                )
            );
    }
}
