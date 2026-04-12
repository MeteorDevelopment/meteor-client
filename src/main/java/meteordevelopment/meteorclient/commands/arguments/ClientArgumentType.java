/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import java.util.Arrays;
import java.util.Collection;

public class ClientArgumentType implements ArgumentType<String> {
    private static final String[] SUPPORTED_CLIENTS = { "mio", "wurst" };

    public static ClientArgumentType create() {
        return new ClientArgumentType();
    }

    public static String get(CommandContext<?> context, String name) throws CommandSyntaxException {
        String client = context.getArgument(name, String.class).toLowerCase();
        for (String s : SUPPORTED_CLIENTS) {
            if (s.equals(client)) return s;
        }
        throw new SimpleCommandExceptionType(() -> "Unknown client: " + client).create();
    }

    @Override
    public String parse(com.mojang.brigadier.StringReader reader) {
        return reader.readUnquotedString();
    }

    public Collection<String> getExamples() {
        return Arrays.asList(SUPPORTED_CLIENTS);
    }
}
