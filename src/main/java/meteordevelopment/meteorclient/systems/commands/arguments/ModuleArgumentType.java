/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ModuleArgumentType implements ArgumentType<Module> {
    private static final DynamicCommandExceptionType NO_SUCH_MODULE = new DynamicCommandExceptionType(name -> Text.literal("Module with name " + name + " doesn't exist."));

    private static final Collection<String> EXAMPLES = Modules.get().getAll()
            .stream()
            .limit(3)
            .map(module -> module.name)
            .collect(Collectors.toList());

    public static ModuleArgumentType create() {
        return new ModuleArgumentType();
    }

    public static Module get(CommandContext<?> context) {
        return context.getArgument("module", Module.class);
    }

    @Override
    public Module parse(StringReader reader) throws CommandSyntaxException {
        String argument = reader.readString();
        Module module = Modules.get().get(argument);
        if (module == null) throw NO_SUCH_MODULE.create(argument);

        return module;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(Modules.get().getAll().stream().map(module -> module.name), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
