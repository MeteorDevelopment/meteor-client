/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
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
import minegame159.meteorclient.friends.Friend;
import minegame159.meteorclient.friends.Friends;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static net.minecraft.command.CommandSource.suggestMatching;

public class FriendCommand extends Command {

    public FriendCommand() {
        super("friend", "Manages friends.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("add")
                .then(argument("friend", FriendArgumentType.friend())
                        .executes(context -> {
                            Friend friend = FriendArgumentType.getFriend(context, "friend");

                            if (Friends.get().add(friend)) ChatUtils.prefixInfo("Friends","Added (highlight)%s (default)to friends.", friend.name);
                            else ChatUtils.prefixError("Friends","That person is already your friend.");

                            return SINGLE_SUCCESS;
                        })))
                .then(literal("remove").then(argument("friend", FriendArgumentType.friend())
                        .executes(context -> {
                            Friend friend = FriendArgumentType.getFriend(context, "friend");

                            if (Friends.get().remove(friend)) ChatUtils.prefixInfo("Friends","Removed (highlight)%s (default)from friends.", friend.name);
                            else ChatUtils.prefixError("Friends", "That person is not your friend.");

                            return SINGLE_SUCCESS;
                        })))
                .then(literal("list").executes(context -> {
                    ChatUtils.prefixInfo("Friends","You have (highlight)%d (default)friends:", Friends.get().count());
                    Friends.get().forEach(friend-> ChatUtils.info(" - (highlight)%s", friend.name));

                    return SINGLE_SUCCESS;
                }));
    }

    private static class FriendArgumentType implements ArgumentType<Friend> {

        public static FriendArgumentType friend() {
            return new FriendArgumentType();
        }

        @Override
        public Friend parse(StringReader reader) throws CommandSyntaxException {
            return new Friend(reader.readString());
        }

        public static Friend getFriend(CommandContext<?> context, String name) {
            return context.getArgument(name, Friend.class);
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
            return suggestMatching(mc.getNetworkHandler().getPlayerList().stream()
                    .map(entry -> entry.getProfile().getName()).collect(Collectors.toList()), builder);
        }

        @Override
        public Collection<String> getExamples() {
            return Arrays.asList("seasnail8169", "MineGame159");
        }
    }

}
