/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.commands.commands.swarm;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.commands.arguments.ModuleArgumentType;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.misc.Swarm;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class SwarmModuleToggle extends Command {

    public SwarmModuleToggle() {
        super("swarm", "(highlight)module <module> <true/false>(default) - Toggle a module on or off.");
    }


    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("module").then(argument("m", ModuleArgumentType.module()).then(argument("bool", BoolArgumentType.bool()).executes(context -> {
            Swarm swarm = Modules.get().get(Swarm.class);
            if (swarm.currentMode == Swarm.Mode.Queen && swarm.server != null) {
                swarm.server.sendMessage(context.getInput());
            } else {
                Module module = context.getArgument("m", Module.class);
                if (module.isActive() != context.getArgument("bool", Boolean.class)) {
                    module.toggle();
                }
            }
            return SINGLE_SUCCESS;
        }))));
    }
}
