/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.commands.arguments.CommandArgumentType;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;

public class HelpCommand extends Command {
    public HelpCommand() {
        super("help", "Shows you what a command does.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("command", CommandArgumentType.create()).executes(context -> {
            showHelp(CommandArgumentType.get(context));
            return SINGLE_SUCCESS;
        }));

        builder.executes(context -> {
            showHelp(this);
            return SINGLE_SUCCESS;
        });
    }

    private void showHelp(Command cmd) {
        MutableText msg = Text.literal("");
        msg.append(Text.literal("Help for ").formatted(Formatting.GRAY).append(Text.literal(cmd.getName()).formatted(Formatting.YELLOW)));
        msg.append(Text.literal("\n ")).append(Text.literal("Description: ").formatted(Formatting.GRAY).append(Text.literal(cmd.getDescription()).formatted(Formatting.WHITE)));

        if (!cmd.getAliases().isEmpty()) {
            msg.append(Text.literal("\n ")).append(Text.literal("Aliases: ").formatted(Formatting.GRAY));
            msg.append(Text.literal(String.join(", ", cmd.getAliases())).formatted(Formatting.AQUA));
        }

        msg.append(getUsageText(cmd));
        ChatUtils.sendMsg(msg);
    }

    private MutableText getUsageText(Command cmd) {
        CommandSource source = mc.getNetworkHandler().getCommandSource();
        CommandNode<CommandSource> root = Commands.DISPATCHER.getRoot();
        CommandNode<CommandSource> node = root.getChild(cmd.getName());

        MutableText usagesText = Text.literal("");

        if (node != null) {
            Map<CommandNode<CommandSource>, String> usages = Commands.DISPATCHER.getSmartUsage(node, source);

            for (String usage : usages.values()) {
                usagesText.append(Text.literal("\n " + cmd + " ").formatted(Formatting.GREEN)).append(Text.literal(usage).formatted(Formatting.GREEN));
            }
        }

        if (usagesText.getString().isEmpty()) {
            usagesText.append(Text.literal("\n " + cmd).formatted(Formatting.GREEN));
        }

        return Text.literal("\n Usage:").formatted(Formatting.GRAY).append(usagesText);
    }
}
