/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.function.Function;

/**
 * A {@link Function} like functional interface that maps a {@link CommandContext} to a command argument.
 */
@FunctionalInterface
public interface ArgumentFunction<S, T> {
    T apply(CommandContext<S> context) throws CommandSyntaxException;

    @SuppressWarnings("unchecked")
    default <S1> T uncheckedApply(CommandContext<S1> context) throws CommandSyntaxException {
        ArgumentFunction<S1, T> castedArgument = (ArgumentFunction<S1, T>) this;
        return castedArgument.apply(context);
    }

    default <V> ArgumentFunction<S, V> andThen(Function<? super T, ? extends V> after) {
        return context -> after.apply(this.apply(context));
    }
}
