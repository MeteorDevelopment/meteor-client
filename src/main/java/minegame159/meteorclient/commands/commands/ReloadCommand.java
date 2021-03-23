/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.systems.Systems;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class ReloadCommand extends Command {
    public ReloadCommand() {
        super("reload", "Reloads the config, modules, friends, macros and accounts.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            Systems.load();

            return SINGLE_SUCCESS;
        });
    }
}
