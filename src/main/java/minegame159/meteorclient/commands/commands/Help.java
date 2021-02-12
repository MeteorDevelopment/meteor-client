/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.commands.Commands;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class Help extends Command {
    public Help() {
        super("help", "List of all commands.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            ChatUtils.info("--- List of all (highlight)%d(default) commands ---", Commands.get().getCount());
            Commands.get().forEach(command -> ChatUtils.info("(highlight)%s(default): %s", command.getName(), command.getDescription()));
            return SINGLE_SUCCESS;
        });
    }
}