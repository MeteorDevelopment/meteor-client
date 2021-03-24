/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.commands.arguments.ModuleArgumentType;
import minegame159.meteorclient.modules.Module;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class ToggleCommand extends Command {


    public ToggleCommand() {
        super("toggle", "Toggles a module.", "t");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("module", ModuleArgumentType.module())
                .executes(context -> {
                    Module m = context.getArgument("module", Module.class);
                    m.toggle();
                    m.sendToggledMsg();
                    return SINGLE_SUCCESS;
                }).then(literal("on")
                        .executes(context -> {
                            Module m = context.getArgument("module", Module.class);
                            if (!m.isActive()) m.toggle(); m.sendToggledMsg();
                            return SINGLE_SUCCESS;
                        })).then(literal("off")
                        .executes(context -> {
                            Module m = context.getArgument("module", Module.class);
                            if (m.isActive()) m.toggle(); m.sendToggledMsg();
                            return SINGLE_SUCCESS;
                        })
                )
        );
    }
}
