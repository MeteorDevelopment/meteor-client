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
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.LogoutSpots;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LogoutPlayerArgumentType implements ArgumentType<String> {
    private static final LogoutPlayerArgumentType INSTANCE = new LogoutPlayerArgumentType();

    private static final Collection<String> EXAMPLES = List.of("seasnail8169", "MineGame159");

    public static LogoutPlayerArgumentType create() {
        return INSTANCE;
    }

    public static String get(CommandContext<?> context) {
        return context.getArgument("name", String.class);
    }

    private LogoutPlayerArgumentType() {
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return reader.readString();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        LogoutSpots logoutSpots = Modules.get().get(LogoutSpots.class);
        return SharedSuggestionProvider.suggest(logoutSpots.getLoggedOutPlayerNames(), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
