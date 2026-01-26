/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.Commands;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CommandArgumentType implements ArgumentType<Command> {
    private static final CommandArgumentType INSTANCE = new CommandArgumentType();
    private static final DynamicCommandExceptionType NO_SUCH_COMMAND = new DynamicCommandExceptionType(name -> Text.literal("Command with name " + name + " doesn't exist."));
    private static final Collection<String> EXAMPLES = Commands.COMMANDS.stream().limit(3).map(Command::getName).collect(Collectors.toList());

    private CommandArgumentType() {
    }

    public static CommandArgumentType create() {
        return INSTANCE;
    }

    public static Command get(CommandContext<?> context) {
        return context.getArgument("command", Command.class);
    }

    @Override
    public Command parse(StringReader reader) throws CommandSyntaxException {
        String name = reader.readString();

        Command command = Commands.get(name);

        if (command == null) {
            for (Command c : Commands.COMMANDS) {
                for (String alias : c.getAliases()) {
                    if (alias.equals(name)) {
                        command = c;
                        break;
                    }
                }
                if (command != null) break;
            }
        }

        if (command == null) throw NO_SUCH_COMMAND.create(name);
        return command;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        Set<String> suggestions = new LinkedHashSet<>();
        for (Command c : Commands.COMMANDS) {
            suggestions.add(c.getName());
            suggestions.addAll(c.getAliases());
        }
        return CommandSource.suggestMatching(suggestions, builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
