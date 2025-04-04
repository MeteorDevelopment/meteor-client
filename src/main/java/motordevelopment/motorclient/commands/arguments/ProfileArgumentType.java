/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.commands.arguments;

import com.google.common.collect.Streams;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import motordevelopment.motorclient.systems.profiles.Profile;
import motordevelopment.motorclient.systems.profiles.Profiles;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.command.CommandSource.suggestMatching;

public class ProfileArgumentType implements ArgumentType<String> {
    private static final ProfileArgumentType INSTANCE = new ProfileArgumentType();
    private static final DynamicCommandExceptionType NO_SUCH_PROFILE = new DynamicCommandExceptionType(name -> Text.literal("Profile with name " + name + " doesn't exist."));

    private static final Collection<String> EXAMPLES = List.of("pvp.motorclient.com", "anarchy");

    public static ProfileArgumentType create() {
        return INSTANCE;
    }

    public static Profile get(CommandContext<?> context) {
        return Profiles.get().get(context.getArgument("profile", String.class));
    }

    private ProfileArgumentType() {}

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        String argument = reader.getRemaining();
        reader.setCursor(reader.getTotalLength());
        if (Profiles.get().get(argument) == null) throw NO_SUCH_PROFILE.create(argument);

        return argument;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return suggestMatching(Streams.stream(Profiles.get()).map(profile -> profile.name.get()), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
