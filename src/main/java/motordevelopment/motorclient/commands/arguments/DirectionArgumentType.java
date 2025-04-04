/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.commands.arguments;

import net.minecraft.command.argument.EnumArgumentType;
import net.minecraft.util.math.Direction;

public class DirectionArgumentType extends EnumArgumentType<Direction> {
    private static final DirectionArgumentType INSTANCE = new DirectionArgumentType();

    private DirectionArgumentType() {
        super(Direction.CODEC, Direction::values);
    }

    public static DirectionArgumentType create() {
        return INSTANCE;
    }
}
