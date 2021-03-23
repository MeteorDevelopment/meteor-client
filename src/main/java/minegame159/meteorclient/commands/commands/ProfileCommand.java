/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.utils.files.ProfileUtils;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.text.LiteralText;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class ProfileCommand extends Command {

    public ProfileCommand() {
        super("profile", "Loads and saves profiles.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {

        builder.then(literal("load")
                .then(argument("profile", ProfileArgumentType.profile()).executes(context -> {
                    String profile = context.getArgument("profile", String.class);

                    ProfileUtils.load(profile);
                    ChatUtils.prefixInfo("Profiles","Loaded profile (highlight)" + profile + "(default).");

                    return SINGLE_SUCCESS;
                })));

        builder.then(literal("save")
                .then(argument("name", StringArgumentType.string()).executes(context -> {
                    String profile = context.getArgument("name", String.class);

                    ProfileUtils.save(profile);
                    ChatUtils.prefixInfo("Profiles","Saved profile (highlight)" + profile + "(default).");

                    return SINGLE_SUCCESS;
                })));

        builder.then(literal("delete")
                .then(argument("profile", ProfileArgumentType.profile()).executes(context -> {
                    String profile = context.getArgument("profile", String.class);

                    ProfileUtils.delete(profile);
                    ChatUtils.prefixInfo("Profiles","Deleted profile (highlight)" + profile + "(default).");

                    return SINGLE_SUCCESS;
                })));
    }

    public static class ProfileArgumentType implements ArgumentType<String> {
        private static final DynamicCommandExceptionType NO_SUCH_PROFILE = new DynamicCommandExceptionType(name ->
                new LiteralText("Profile with name " + name + " doesn't exist."));

        public static ProfileArgumentType profile() {
            return new ProfileArgumentType();
        }

        @Override
        public String parse(StringReader reader) throws CommandSyntaxException {
            String argument = reader.readString();

            boolean isProfile = ProfileUtils.isProfile(argument);

            if (!isProfile) throw NO_SUCH_PROFILE.create(argument);

            return argument;
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
            return CommandSource.suggestMatching(ProfileUtils.getProfiles(), builder);
        }

        private final Collection<String> EXAMPLES = new ArrayList<>(ProfileUtils.getProfiles());

        @Override
        public Collection<String> getExamples() {
            return EXAMPLES;
        }
    }
}
