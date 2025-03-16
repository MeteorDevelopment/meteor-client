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
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.commands.PathReader;
import meteordevelopment.meteorclient.utils.notebot.decoder.SongDecoders;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class NotebotSongArgumentType implements ArgumentType<Path> {
    private static final NotebotSongArgumentType INSTANCE = new NotebotSongArgumentType();
    private static final DynamicCommandExceptionType INVALID_SONG_EXCEPTION = new DynamicCommandExceptionType(path -> new LiteralMessage("Path " + path + " is not a valid song."));

    public static NotebotSongArgumentType create() {
        return INSTANCE;
    }

    private NotebotSongArgumentType() {}

    public static <S> Path get(CommandContext<S> context) {
        return context.getArgument("song", Path.class);
    }

    public static <S> Path get(CommandContext<S> context, String name) {
        return context.getArgument(name, Path.class);
    }

    @Override
    public Path parse(StringReader reader) throws CommandSyntaxException {
        Path pathArgument = PathReader.readPath(reader);
        Path path = MeteorClient.FOLDER.toPath().resolve("notebot").resolve(pathArgument);

        if (!Files.exists(path)) {
            throw INVALID_SONG_EXCEPTION.create(pathArgument.toString());
        }

        return path;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        try (var suggestions = Files.list(MeteorClient.FOLDER.toPath().resolve("notebot"))) {
            return CommandSource.suggestMatching(suggestions
                    .filter(SongDecoders::hasDecoder)
                    .map(path -> path.getFileName().toString()),
                builder
            );
        } catch (IOException e) {
            return Suggestions.empty();
        }
    }
}
