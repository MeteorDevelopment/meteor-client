/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.systems.commands.Command;
import minegame159.meteorclient.systems.commands.Commands;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Formatting;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class HelpCommand extends Command {
    public HelpCommand() {
        super("help", "List of all commands.", "commands");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            ChatUtils.info("--- List of all (highlight)%d(default) commands ---", Commands.get().getCount());

            Commands.get().forEach(command -> {
                if (command.getAliases().size() >= 1) {
                    StringBuilder sb = new StringBuilder();

                    sb.append(Formatting.AQUA).append("[");
                    command.getAliases().forEach(alias -> {
                        sb.append(alias);
                        if (!command.getAliases().get(command.getAliases().size() - 1).equals(alias)) sb.append(", ");
                    });
                    sb.append("]").append(Formatting.RESET);

                    ChatUtils.info("(highlight)%s %s(default): %s", command.getName(), sb.toString(), command.getDescription());
                }
                else {
                    ChatUtils.info("(highlight)%s(default): %s", command.getName(), command.getDescription());
                }
            });
            return SINGLE_SUCCESS;
        });
    }
}