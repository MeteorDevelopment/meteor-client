/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.systems.commands.arguments.MacroArgumentType;
import meteordevelopment.meteorclient.systems.macros.Macro;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class MacroCommand extends Command {
    public MacroCommand() {
        super("macro", "Allows you to execute macros.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(
            argument("macro", MacroArgumentType.create())
                .executes(context -> {
                    Macro macro = context.getArgument("macro", Macro.class);
                    macro.onAction();
                    return SINGLE_SUCCESS;
                }));
    }
}
