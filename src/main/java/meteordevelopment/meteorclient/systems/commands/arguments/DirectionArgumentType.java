/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands.arguments;

import net.minecraft.command.argument.EnumArgumentType;
import net.minecraft.util.math.Direction;

public class DirectionArgumentType extends EnumArgumentType<Direction> {
    private DirectionArgumentType() {
        super(Direction.CODEC, Direction::values);
    }

    public static DirectionArgumentType create() {
        return new DirectionArgumentType();
    }
}
