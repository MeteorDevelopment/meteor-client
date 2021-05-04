/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.systems.commands.Command;
import minegame159.meteorclient.systems.commands.Commands;
import minegame159.meteorclient.systems.config.Config;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.text.BaseText;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class CommandsCommand extends Command {
    public CommandsCommand() {
        super("help", "List of all commands.", "commands");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            ChatUtils.info("--- (highlight)%d(default) Commands ---", Commands.get().getCount());

            BaseText commands = new LiteralText("");

            for (Command command : Commands.get().getAll()) {
                BaseText commandTooltip = new LiteralText("");

                // Name
                commandTooltip.append(new LiteralText(Utils.nameToTitle(command.getName())).formatted(Formatting.BLUE, Formatting.BOLD)).append("\n");

                // Aliases
                BaseText aliases = new LiteralText(Config.get().prefix + command.getName());
                if (command.getAliases().size() > 0) {
                    aliases.append(", ");
                    for (String alias : command.getAliases()) {
                        if (alias.isEmpty()) continue;
                        aliases.append(Config.get().prefix + alias);
                        if (!alias.equals(command.getAliases().get(command.getAliases().size() - 1))) aliases.append(", ");
                    }
                }
                commandTooltip.append(aliases.formatted(Formatting.GRAY)).append("\n\n");

                // Description
                commandTooltip.append(new LiteralText(command.getDescription()).formatted(Formatting.WHITE));

                BaseText finalCommand = new LiteralText(Utils.nameToTitle(command.getName()));
                if (command != Commands.get().getAll().get(Commands.get().getAll().size() - 1)) finalCommand.append(new LiteralText(", ").formatted(Formatting.GRAY));
                finalCommand.setStyle(finalCommand
                        .getStyle()
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, commandTooltip))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, Config.get().prefix + command.getName()))
                );

                commands.append(finalCommand);
            }

            ChatUtils.info(commands);

            return SINGLE_SUCCESS;
        });
    }

}