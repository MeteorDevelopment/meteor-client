/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import motordevelopment.motorclient.commands.Command;
import motordevelopment.motorclient.commands.arguments.ModuleArgumentType;
import motordevelopment.motorclient.systems.modules.Module;
import motordevelopment.motorclient.systems.modules.Modules;
import net.minecraft.command.CommandSource;

public class BindCommand extends Command {
    public BindCommand() {
        super("bind", "Binds a specified module to the next pressed key.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("module", ModuleArgumentType.create()).executes(context -> {
            Module module = context.getArgument("module", Module.class);
            Modules.get().setModuleToBind(module);
            Modules.get().awaitKeyRelease();
            module.info("Press a key to bind the module to.");
            return SINGLE_SUCCESS;
        }));
    }
}
