/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.utils.Chat;
import net.minecraft.command.CommandSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static minegame159.meteorclient.utils.Utils.mc;
import static net.minecraft.command.CommandSource.suggestMatching;

public class Friend extends Command {
    public Friend() {
        super("friend", "Manages friends.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("add")
                .then(argument("friend", FriendArgumentType.friend())
                        .executes(context -> {
                            minegame159.meteorclient.friends.Friend friend =
                                    context.getArgument("friend", minegame159.meteorclient.friends.Friend.class);
                            if (FriendManager.INSTANCE.add(friend)) {
                                Chat.info("Added (highlight)%s (default)to friends.", friend.name);
                            } else {
                                Chat.error("That person is already your friend.");
                            }

                            return SINGLE_SUCCESS;
                        })))
                .then(literal("remove").then(argument("friend", FriendArgumentType.friend())
                        .executes(context -> {
                            minegame159.meteorclient.friends.Friend friend =
                                    context.getArgument("friend", minegame159.meteorclient.friends.Friend.class);
                            if (FriendManager.INSTANCE.remove(friend)) {
                                Chat.info("Removed (highlight)%s (default)from friends.", friend.name);
                            } else {
                                Chat.error("That person is not your friend.");
                            }

                            return SINGLE_SUCCESS;
                        })))
                .then(literal("list").executes(context -> {
                    Chat.info("You have (highlight)%d (default)friends:", FriendManager.INSTANCE.count());

                    for (minegame159.meteorclient.friends.Friend friend : FriendManager.INSTANCE) {
                        Chat.info(" - (highlight)%s", friend.name);
                    }

                    return SINGLE_SUCCESS;
                }));
    }

    private static class FriendArgumentType implements ArgumentType<minegame159.meteorclient.friends.Friend> {

        public static FriendArgumentType friend() {
            return new FriendArgumentType();
        }

        @Override
        public minegame159.meteorclient.friends.Friend parse(StringReader reader) throws CommandSyntaxException {
            return new minegame159.meteorclient.friends.Friend(reader.readString());
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
            return suggestMatching(mc.getNetworkHandler().getPlayerList().stream()
                    .map(entry -> entry.getProfile().getName()).collect(Collectors.toList()), builder);
        }

        @Override
        public Collection<String> getExamples() {
            // :)
            return Arrays.asList("086", "seasnail8169", "squidoodly");
        }
    }

}
