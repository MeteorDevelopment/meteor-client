/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import motordevelopment.motorclient.systems.macros.Macro;
import motordevelopment.motorclient.systems.macros.Macros;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MacroArgumentType implements ArgumentType<Macro> {
    private static final MacroArgumentType INSTANCE = new MacroArgumentType();
    private static final DynamicCommandExceptionType NO_SUCH_MACRO = new DynamicCommandExceptionType(name -> Text.literal("Macro with name " + name + " doesn't exist."));

    public static MacroArgumentType create() {
        return INSTANCE;
    }

    public static Macro get(CommandContext<?> context) {
        return context.getArgument("macro", Macro.class);
    }

    private MacroArgumentType() {}

    @Override
    public Macro parse(StringReader reader) throws CommandSyntaxException {
        String argument = reader.readString();
        Macro macro = Macros.get().get(argument);
        if (macro == null) throw NO_SUCH_MACRO.create(argument);

        return macro;
    }

    @Override
    public CompletableFuture<Suggestions> listSuggestions(CommandContext context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(Macros.get().getAll().stream().map(macro -> macro.name.get()), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return Macros.get().getAll().stream().limit(3).map(macro -> macro.name.get()).collect(Collectors.toList());
    }
}
