/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.arguments;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.argument.EnumArgumentType;
import net.minecraft.util.math.Direction;

public class DirectionArgumentType extends EnumArgumentType<Direction> {
    private static final DirectionArgumentType INSTANCE = new DirectionArgumentType();

    private DirectionArgumentType() {
        super(Direction.CODEC, Direction::values);
    }

    public static <S> Direction getDirection(CommandContext<S> context) {
        return context.getArgument("direction", Direction.class);
    }

    public static <S> Direction getDirection(CommandContext<S> context, String name) {
        return context.getArgument(name, Direction.class);
    }

    public static DirectionArgumentType create() {
        return INSTANCE;
    }
}
