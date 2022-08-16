/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands.commands;

import com.google.common.collect.Streams;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.systems.commands.arguments.PlayerArgumentType;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static net.minecraft.command.CommandSource.suggestMatching;

public class FriendsCommand extends Command {

    public FriendsCommand() {
        super("friends", "Manages friends.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("add").then(argument("player", PlayerArgumentType.player())
                        .executes(context -> {
                            Friend friend = new Friend(PlayerArgumentType.getPlayer(context));

                            if (Friends.get().add(friend)) info("Added (highlight)%s (default)to friends.", friend.name);
                            else error("That person is already your friend.");

                            return SINGLE_SUCCESS;
                        })
                )
        );

        builder.then(literal("remove").then(argument("friend", FriendArgumentType.friend())
                        .executes(context -> {
                            Friend friend = FriendArgumentType.getFriend(context, "friend");

                            if (Friends.get().remove(friend)) info("Removed (highlight)%s (default)from friends.", friend.name);
                            else error("That person is not your friend.");

                            return SINGLE_SUCCESS;
                        })
                )
        );

        builder.then(literal("list").executes(context -> {
                    info("--- Friends ((highlight)%s(default)) ---", Friends.get().count());
                    Friends.get().forEach(friend-> ChatUtils.info("(highlight)" + friend.name));
                    return SINGLE_SUCCESS;
                })
        );
    }

    private static class FriendArgumentType implements ArgumentType<String> {
        public static FriendArgumentType friend() {
            return new FriendArgumentType();
        }

        @Override
        public String parse(StringReader reader) throws CommandSyntaxException {
            return reader.readString();
        }

        public static Friend getFriend(CommandContext<?> context, String name) {
            String friendName = context.getArgument(name, String.class);

            for (Friend friend : Friends.get()) {
                if (friend.name.equals(friendName)) return friend;
            }

            return null;
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
            return suggestMatching(
                Streams.stream(Friends.get()).map(friend -> friend.name),
                builder
            );
        }

        @Override
        public Collection<String> getExamples() {
            return Arrays.asList("seasnail8169", "MineGame159");
        }
    }
}
