/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.commands.arguments.ModuleArgumentType;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class Bind extends Command {
    public Bind() {
        super("bind", "Binds a module to a specified key.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("module", ModuleArgumentType.module())
                .executes(context -> {
                    Module m = context.getArgument("module", Module.class);

                    ChatUtils.prefixInfo("KeyBinds","Press a key you want this module to be bound to.");
                    Modules.get().setModuleToBind(m);

                    return SINGLE_SUCCESS;
                }));
    }
}