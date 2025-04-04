/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import motordevelopment.motorclient.commands.Command;
import motordevelopment.motorclient.commands.arguments.MacroArgumentType;
import motordevelopment.motorclient.systems.macros.Macro;
import net.minecraft.command.CommandSource;

public class MacroCommand extends Command {
    public MacroCommand() {
        super("macro", "Allows you to execute macros.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("macro", MacroArgumentType.create()).executes(context -> {
            Macro macro = MacroArgumentType.get(context);
            macro.onAction();
            return SINGLE_SUCCESS;
        }));
    }
}
