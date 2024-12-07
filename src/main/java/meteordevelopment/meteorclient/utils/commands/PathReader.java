/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.commands;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class PathReader {
    private static final SimpleCommandExceptionType EMPTY_PATH_EXCEPTION = new SimpleCommandExceptionType(new LiteralMessage("Path cannot be empty."));
    private static final DynamicCommandExceptionType INVALID_PATH_EXCEPTION = new DynamicCommandExceptionType(path -> new LiteralMessage("Path " + path + " is not valid."));

    public static Path readPath(StringReader reader) throws CommandSyntaxException {
        if (!reader.canRead()) {
            throw EMPTY_PATH_EXCEPTION.create();
        }

        String pathString = readPathString(reader);

        try {
            return Path.of(pathString);
        } catch (InvalidPathException e) {
            throw INVALID_PATH_EXCEPTION.create(pathString);
        }
    }

    public static boolean isAllowedInUnquotedPath(char c) {
        return StringReader.isAllowedInUnquotedString(c) || c == '\\' || c == '/';
    }

    private static String readPathString(StringReader reader) throws CommandSyntaxException {
        char next = reader.peek();
        if (StringReader.isQuotedStringStart(next)) {
            reader.skip();
            return reader.readStringUntil(next);
        }

        int start = reader.getCursor();
        while (reader.canRead() && isAllowedInUnquotedPath(reader.peek())) {
            reader.skip();
        }
        return reader.getString().substring(start, reader.getCursor());
    }
}
