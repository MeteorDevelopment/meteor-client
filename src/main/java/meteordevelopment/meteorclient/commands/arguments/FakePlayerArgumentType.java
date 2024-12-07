/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.arguments;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerManager;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.command.CommandSource.suggestMatching;

public class FakePlayerArgumentType implements ArgumentType<FakePlayerEntity> {
    private static final FakePlayerArgumentType INSTANCE = new FakePlayerArgumentType();
    private static final Collection<String> EXAMPLES = List.of("seasnail8169", "MineGame159");
    private static final DynamicCommandExceptionType NO_SUCH_FAKEPLAYER_EXCEPTION = new DynamicCommandExceptionType(name -> new LiteralMessage("Fake player with name " + name + " doesn't exist."));

    public static FakePlayerArgumentType create() {
        return INSTANCE;
    }

    public static <S> FakePlayerEntity get(CommandContext<S> context) {
        return context.getArgument("fp", FakePlayerEntity.class);
    }

    public static <S> FakePlayerEntity get(CommandContext<S> context, String name) {
        return context.getArgument(name, FakePlayerEntity.class);
    }

    private FakePlayerArgumentType() {}

    @Override
    public FakePlayerEntity parse(StringReader reader) throws CommandSyntaxException {
        String name = reader.readString();
        FakePlayerEntity entity = FakePlayerManager.get(name);
        if (entity == null) {
            throw NO_SUCH_FAKEPLAYER_EXCEPTION.create(name);
        }

        return entity;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return suggestMatching(FakePlayerManager.stream().map(fakePlayerEntity -> fakePlayerEntity.getName().getString()), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
