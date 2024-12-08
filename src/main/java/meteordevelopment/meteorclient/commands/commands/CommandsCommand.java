/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

public class CommandsCommand extends Command {
    public CommandsCommand() {
        super("commands", "List of all commands.", "help");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            ChatUtils.info("--- Commands ((highlight)%d(default)) ---", Commands.COMMANDS.size());

            MutableText commands = Text.empty();
            Commands.COMMANDS.forEach(command -> commands.append(getCommandText(command)));
            ChatUtils.sendMsg(commands);

            return SINGLE_SUCCESS;
        });
    }

    private MutableText getCommandText(Command command) {
        // Hover tooltip
        MutableText tooltip = Text.empty();

        tooltip.append(Text.literal(Utils.nameToTitle(command.getName())).formatted(Formatting.BLUE, Formatting.BOLD)).append(ScreenTexts.LINE_BREAK);

        MutableText aliases = Text.literal(Config.get().prefix.get() + command.getName());
        if (!command.getAliases().isEmpty()) {
            aliases.append(Texts.DEFAULT_SEPARATOR_TEXT);
            for (String alias : command.getAliases()) {
                if (alias.isEmpty()) continue;
                aliases.append(Config.get().prefix.get() + alias);
                if (!alias.equals(command.getAliases().getLast())) aliases.append(Texts.DEFAULT_SEPARATOR_TEXT);
            }
        }
        tooltip.append(aliases.formatted(Formatting.GRAY)).append("\n\n");

        tooltip.append(Text.literal(command.getDescription()).formatted(Formatting.WHITE));

        // Text
        MutableText text = Text.literal(Utils.nameToTitle(command.getName()));
        if (command != Commands.COMMANDS.getLast())
            text.append(Texts.GRAY_DEFAULT_SEPARATOR_TEXT);
        text.setStyle(text
            .getStyle()
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip))
            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, Config.get().prefix.get() + command.getName()))
        );

        return text;
    }

}
