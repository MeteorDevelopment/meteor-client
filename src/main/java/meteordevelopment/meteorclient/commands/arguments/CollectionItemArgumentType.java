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
import net.minecraft.command.CommandSource;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

public class CollectionItemArgumentType<T> implements ArgumentType<T> {
    private static final DynamicCommandExceptionType NO_SUCH_ITEM_EXCEPTION = new DynamicCommandExceptionType(o -> new LiteralMessage("No such item '" + o + "'."));
    private final Supplier<Collection<T>> collection;
    private final StringFunction<T> stringFunction;

    public CollectionItemArgumentType(Supplier<Collection<T>> collection, StringFunction<T> stringFunction) {
        this.collection = collection;
        this.stringFunction = stringFunction;
    }

    public CollectionItemArgumentType(Supplier<Collection<T>> collection) {
        this(collection, Object::toString);
    }

    @Override
    public T parse(StringReader stringReader) throws CommandSyntaxException {
        String value = stringReader.readString();

        Optional<T> itemResult = this.collection.get().stream()
            .filter(item -> this.stringFunction.apply(item).equals(value))
            .findAny();

        if (itemResult.isPresent()) {
            return itemResult.get();
        } else {
            throw NO_SUCH_ITEM_EXCEPTION.create(value);
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(this.collection.get().stream().map(this.stringFunction), builder);
    }

    @FunctionalInterface
    public interface StringFunction<T> extends Function<T, String> {
        String apply(T value);
    }
}
