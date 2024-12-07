/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.arguments;

import com.google.common.collect.Streams;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.command.CommandSource.suggestMatching;

public class FriendArgumentType implements ArgumentType<Friend> {
    private static final FriendArgumentType INSTANCE = new FriendArgumentType();
    private static final Collection<String> EXAMPLES = List.of("seasnail8169", "MineGame159");
    private static final DynamicCommandExceptionType NO_SUCH_FRIEND_EXCEPTION = new DynamicCommandExceptionType(name -> new LiteralMessage("Friend with name " + name + " doesn't exist."));

    public static FriendArgumentType create() {
        return INSTANCE;
    }

    public static <S> Friend get(CommandContext<S> context) {
        return context.getArgument("friend", Friend.class);
    }

    public static <S> Friend get(CommandContext<S> context, String name) {
        return context.getArgument(name, Friend.class);
    }

    private FriendArgumentType() {}

    @Override
    public Friend parse(StringReader reader) throws CommandSyntaxException {
        String name = reader.readString();
        Friend friend = Friends.get().get(name);
        if (friend == null) {
            throw NO_SUCH_FRIEND_EXCEPTION.create(name);
        }

        return friend;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return suggestMatching(Streams.stream(Friends.get()).map(Friend::getName), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
