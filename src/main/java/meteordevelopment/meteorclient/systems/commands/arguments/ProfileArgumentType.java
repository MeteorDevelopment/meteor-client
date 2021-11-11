/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import meteordevelopment.meteorclient.systems.profiles.Profile;
import meteordevelopment.meteorclient.systems.profiles.Profiles;
import net.minecraft.command.CommandSource;
import net.minecraft.text.LiteralText;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ProfileArgumentType implements ArgumentType<String> {
    private static final DynamicCommandExceptionType NO_SUCH_PROFILE = new DynamicCommandExceptionType(name -> new LiteralText("Profile with name " + name + " doesn't exist."));

    public static ProfileArgumentType profile() {
        return new ProfileArgumentType();
    }

    public static Profile getProfile(final CommandContext<?> context, final String name) {
        return Profiles.get().get(context.getArgument(name, String.class));
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        String argument = reader.readString();

        if (Profiles.get().get(argument) == null) throw NO_SUCH_PROFILE.create(argument);
        return argument;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(getExamples(), builder);
    }

    @Override
    public Collection<String> getExamples() {
        List<String> names = new ArrayList<>();
        for (Profile profile : Profiles.get()) names.add(profile.name);
        return names;
    }
}
