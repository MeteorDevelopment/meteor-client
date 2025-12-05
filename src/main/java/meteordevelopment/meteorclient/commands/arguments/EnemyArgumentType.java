/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.arguments;

import com.google.common.collect.Streams;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import meteordevelopment.meteorclient.systems.targeting.SavedPlayer;
import meteordevelopment.meteorclient.systems.targeting.Targeting;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.command.CommandSource.suggestMatching;

// TODO: functionality duplication

public class EnemyArgumentType implements ArgumentType<String> {
    private static final EnemyArgumentType INSTANCE = new EnemyArgumentType();
    private static final Collection<String> EXAMPLES = List.of("seasnail8169", "MineGame159");

    public static EnemyArgumentType create() {
        return INSTANCE;
    }

    public static SavedPlayer get(CommandContext<?> context) {
        return Targeting.getFriend(context.getArgument("enemy", String.class));
    }

    private EnemyArgumentType() {}

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return reader.readString();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return suggestMatching(Streams.stream(Targeting.get().getEnemies()).map(SavedPlayer::getName), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
