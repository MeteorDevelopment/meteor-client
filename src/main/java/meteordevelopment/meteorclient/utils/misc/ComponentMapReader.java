/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class ComponentMapReader {
    private static final DynamicCommandExceptionType UNKNOWN_COMPONENT_EXCEPTION = new DynamicCommandExceptionType(
        id -> Component.translatableEscape("arguments.item.component.unknown", id)
    );
    private static final SimpleCommandExceptionType COMPONENT_EXPECTED_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("arguments.item.component.expected"));
    private static final DynamicCommandExceptionType REPEATED_COMPONENT_EXCEPTION = new DynamicCommandExceptionType(
        type -> Component.translatableEscape("arguments.item.component.repeated", type)
    );
    private static final Dynamic2CommandExceptionType MALFORMED_COMPONENT_EXCEPTION = new Dynamic2CommandExceptionType(
        (type, error) -> Component.translatableEscape("arguments.item.component.malformed", type, error)
    );
    private static final TagParser<Tag> SNBT_READER = TagParser.create(NbtOps.INSTANCE);
    private final DynamicOps<Tag> nbtOps;

    public ComponentMapReader(CommandBuildContext commandRegistryAccess) {
        this.nbtOps = commandRegistryAccess.createSerializationContext(NbtOps.INSTANCE);
    }

    public DataComponentMap consume(StringReader reader) throws CommandSyntaxException {
        int cursor = reader.getCursor();

        try {
            return new Reader(reader, nbtOps).read();
        } catch (CommandSyntaxException e) {
            reader.setCursor(cursor);
            throw e;
        }
    }

    public CompletableFuture<Suggestions> getSuggestions(SuggestionsBuilder builder) {
        StringReader stringReader = new StringReader(builder.getInput());
        stringReader.setCursor(builder.getStart());
        Reader reader = new Reader(stringReader, nbtOps);

        try {
            reader.read();
        } catch (CommandSyntaxException ignored) {
        }

        return reader.suggestor.apply(builder.createOffset(stringReader.getCursor()));
    }

    private static class Reader {
        private static final Function<SuggestionsBuilder, CompletableFuture<Suggestions>> SUGGEST_DEFAULT = SuggestionsBuilder::buildFuture;
        private final StringReader reader;
        private final DynamicOps<Tag> nbtOps;
        public Function<SuggestionsBuilder, CompletableFuture<Suggestions>> suggestor = this::suggestBracket;

        public Reader(StringReader reader, DynamicOps<Tag> nbtOps) {
            this.reader = reader;
            this.nbtOps = nbtOps;
        }

        public DataComponentMap read() throws CommandSyntaxException {
            DataComponentMap.Builder builder = DataComponentMap.builder();

            reader.expect('[');
            suggestor = this::suggestComponentType;
            Set<DataComponentType<?>> set = new ReferenceArraySet<>();

            while(reader.canRead() && reader.peek() != ']') {
                reader.skipWhitespace();
                DataComponentType<?> dataComponentType = readComponentType(reader);
                if (!set.add(dataComponentType)) {
                    throw REPEATED_COMPONENT_EXCEPTION.create(dataComponentType);
                }

                suggestor = this::suggestEqual;
                reader.skipWhitespace();
                reader.expect('=');
                suggestor = SUGGEST_DEFAULT;
                reader.skipWhitespace();
                this.readComponentValue(reader, builder, dataComponentType);
                reader.skipWhitespace();
                suggestor = this::suggestEndOfComponent;
                if (!reader.canRead() || reader.peek() != ',') {
                    break;
                }

                reader.skip();
                reader.skipWhitespace();
                suggestor = this::suggestComponentType;
                if (!reader.canRead()) {
                    throw COMPONENT_EXPECTED_EXCEPTION.createWithContext(reader);
                }
            }

            reader.expect(']');
            suggestor = SUGGEST_DEFAULT;

            return builder.build();
        }

        public static DataComponentType<?> readComponentType(StringReader reader) throws CommandSyntaxException {
            if (!reader.canRead()) {
                throw COMPONENT_EXPECTED_EXCEPTION.createWithContext(reader);
            } else {
                int i = reader.getCursor();
                Identifier identifier = Identifier.read(reader);
                DataComponentType<?> dataComponentType = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(identifier);
                if (dataComponentType != null && !dataComponentType.isTransient()) {
                    return dataComponentType;
                } else {
                    reader.setCursor(i);
                    throw UNKNOWN_COMPONENT_EXCEPTION.createWithContext(reader, identifier);
                }
            }
        }

        private CompletableFuture<Suggestions> suggestComponentType(SuggestionsBuilder builder) {
            String string = builder.getRemaining().toLowerCase(Locale.ROOT);
            SharedSuggestionProvider.filterResources(BuiltInRegistries.DATA_COMPONENT_TYPE.entrySet(), string, entry -> entry.getKey().identifier(), entry -> {
                DataComponentType<?> dataComponentType = entry.getValue();
                if (dataComponentType.codec() != null) {
                    Identifier identifier = entry.getKey().identifier();
                    builder.suggest(identifier.toString() + "=");
                }
            });
            return builder.buildFuture();
        }

        private <T> void readComponentValue(StringReader reader, DataComponentMap.Builder builder, DataComponentType<T> type) throws CommandSyntaxException {
            int i = reader.getCursor();
            Tag nbtElement = SNBT_READER.parseAsArgument(reader);
            DataResult<T> dataResult = type.codecOrThrow().parse(this.nbtOps, nbtElement);
            builder.set(type, dataResult.getOrThrow(error -> {
                reader.setCursor(i);
                return MALFORMED_COMPONENT_EXCEPTION.createWithContext(reader, type.toString(), error);
            }));
        }

        private CompletableFuture<Suggestions> suggestBracket(SuggestionsBuilder builder) {
            if (builder.getRemaining().isEmpty()) {
                builder.suggest(String.valueOf('['));
            }

            return builder.buildFuture();
        }

        private CompletableFuture<Suggestions> suggestEndOfComponent(SuggestionsBuilder builder) {
            if (builder.getRemaining().isEmpty()) {
                builder.suggest(String.valueOf(','));
                builder.suggest(String.valueOf(']'));
            }

            return builder.buildFuture();
        }

        private CompletableFuture<Suggestions> suggestEqual(SuggestionsBuilder builder) {
            if (builder.getRemaining().isEmpty()) {
                builder.suggest(String.valueOf('='));
            }

            return builder.buildFuture();
        }
    }
}
