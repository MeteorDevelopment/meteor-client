/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import meteordevelopment.meteorclient.utils.misc.ComponentMapReader;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.component.DataComponentMap;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ComponentMapArgumentType implements ArgumentType<DataComponentMap> {
    private static final Collection<String> EXAMPLES = List.of("{foo=bar}");
    private final ComponentMapReader reader;

    public ComponentMapArgumentType(CommandBuildContext commandRegistryAccess) {
        this.reader = new ComponentMapReader(commandRegistryAccess);
    }

    public static ComponentMapArgumentType componentMap(CommandBuildContext commandRegistryAccess) {
        return new ComponentMapArgumentType(commandRegistryAccess);
    }

    public static <S extends SharedSuggestionProvider> DataComponentMap getComponentMap(CommandContext<S> context, String name) {
        return context.getArgument(name, DataComponentMap.class);
    }

    @Override
    public DataComponentMap parse(StringReader reader) throws CommandSyntaxException {
        return this.reader.consume(reader);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return this.reader.getSuggestions(builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
