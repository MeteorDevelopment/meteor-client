/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import motordevelopment.motorclient.MotorClient;
import motordevelopment.motorclient.utils.notebot.decoder.SongDecoders;
import net.minecraft.command.CommandSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class NotebotSongArgumentType implements ArgumentType<Path> {
    private static final NotebotSongArgumentType INSTANCE = new NotebotSongArgumentType();

    public static NotebotSongArgumentType create() {
        return INSTANCE;
    }

    private NotebotSongArgumentType() {}

    @Override
    public Path parse(StringReader reader) throws CommandSyntaxException {
        final String text = reader.getRemaining();
        reader.setCursor(reader.getTotalLength());
        return MotorClient.FOLDER.toPath().resolve("notebot/" + text);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        try (var suggestions = Files.list(MotorClient.FOLDER.toPath().resolve("notebot"))) {
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
