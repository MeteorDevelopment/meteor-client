/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.commands.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import minegame159.meteorclient.systems.commands.Command;
import minegame159.meteorclient.systems.profiles.Profile;
import minegame159.meteorclient.systems.profiles.Profiles;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.text.LiteralText;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class ProfileCommand extends Command {

    public ProfileCommand() {
        super("profile", "Loads and saves profiles.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("profile", ProfileArgumentType.profile())
                .then(literal("load").executes(context -> {
                    Profile profile = ProfileArgumentType.getProfile(context, "profile");

                    if (profile != null) {
                        profile.load();
                        ChatUtils.prefixInfo("Profiles","Loaded profile (highlight)%s(default).", profile.name);
                    }

                    return SINGLE_SUCCESS;
                }))
                .then(literal("save").executes(context -> {
                    Profile profile = ProfileArgumentType.getProfile(context, "profile");

                    if (profile != null) {
                        profile.save();
                        ChatUtils.prefixInfo("Profiles","Saved profile (highlight)%s(default).", profile.name);
                    }

                    return SINGLE_SUCCESS;
                }))
                .then(literal("delete").executes(context -> {
                    Profile profile = ProfileArgumentType.getProfile(context, "profile");

                    if (profile != null) {
                        Profiles.get().remove(profile);
                        ChatUtils.prefixInfo("Profiles","Deleted profile (highlight)%s(default).", profile.name);
                    }

                    return SINGLE_SUCCESS;
                })));
    }

    public static class ProfileArgumentType implements ArgumentType<String> {
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

}
