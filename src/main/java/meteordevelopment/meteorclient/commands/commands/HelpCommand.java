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
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import java.util.Map;

public class HelpCommand extends Command {
    public HelpCommand() {
        super("help", "Shows you what a command does.");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
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
        MutableComponent msg = Component.literal("");
        msg.append(Component.literal("Help for ").withStyle(ChatFormatting.GRAY).append(Component.literal(cmd.getName()).withStyle(ChatFormatting.YELLOW)));
        msg.append(Component.literal("\n ")).append(Component.literal("Description: ").withStyle(ChatFormatting.GRAY).append(Component.literal(cmd.getDescription()).withStyle(ChatFormatting.WHITE)));

        if (!cmd.getAliases().isEmpty()) {
            msg.append(Component.literal("\n ")).append(Component.literal("Aliases: ").withStyle(ChatFormatting.GRAY));
            msg.append(Component.literal(String.join(", ", cmd.getAliases())).withStyle(ChatFormatting.AQUA));
        }

        msg.append(getUsageText(cmd));
        ChatUtils.sendMsg(msg);
    }

    private MutableComponent getUsageText(Command cmd) {
        SharedSuggestionProvider source = mc.getConnection().getSuggestionsProvider();
        CommandNode<SharedSuggestionProvider> root = Commands.DISPATCHER.getRoot();
        CommandNode<SharedSuggestionProvider> node = root.getChild(cmd.getName());

        MutableComponent usagesText = Component.literal("");

        if (node != null) {
            Map<CommandNode<SharedSuggestionProvider>, String> usages = Commands.DISPATCHER.getSmartUsage(node, source);

            for (String usage : usages.values()) {
                usagesText.append(Component.literal("\n " + cmd + " ").withStyle(ChatFormatting.GREEN)).append(Component.literal(usage).withStyle(ChatFormatting.GREEN));
            }
        }

        if (usagesText.getString().isEmpty()) {
            usagesText.append(Component.literal("\n " + cmd).withStyle(ChatFormatting.GREEN));
        }

        return Component.literal("\n Usage:").withStyle(ChatFormatting.GRAY).append(usagesText);
    }
}
