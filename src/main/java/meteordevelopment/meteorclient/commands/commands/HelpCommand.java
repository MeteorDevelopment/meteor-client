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
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

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
        MutableComponent msg = MutableComponent.literal("");
        msg.append(MutableComponent.literal("Help for ").formatted(ChatFormatting.GRAY).append(MutableComponent.literal(cmd.getName()).formatted(ChatFormatting.YELLOW)));
        msg.append(MutableComponent.literal("\n ")).append(MutableComponent.literal("Description: ").formatted(ChatFormatting.GRAY).append(MutableComponent.literal(cmd.getDescription()).formatted(ChatFormatting.WHITE)));

        if (!cmd.getAliases().isEmpty()) {
            msg.append(MutableComponent.literal("\n ")).append(MutableComponent.literal("Aliases: ").formatted(ChatFormatting.GRAY));
            msg.append(MutableComponent.literal(String.join(", ", cmd.getAliases())).formatted(ChatFormatting.AQUA));
        }

        msg.append(getUsageText(cmd));
        ChatUtils.sendMsg(msg);
    }

    private MutableComponent getUsageText(Command cmd) {
        SharedSuggestionProvider source = mc.getNetworkHandler().getCommandSource();
        CommandNode<SharedSuggestionProvider> root = Commands.DISPATCHER.getRoot();
        CommandNode<SharedSuggestionProvider> node = root.getChild(cmd.getName());

        MutableComponent usagesText = MutableComponent.literal("");

        if (node != null) {
            Map<CommandNode<SharedSuggestionProvider>, String> usages = Commands.DISPATCHER.getSmartUsage(node, source);

            for (String usage : usages.values()) {
                usagesText.append(MutableComponent.literal("\n " + cmd + " ").formatted(ChatFormatting.GREEN)).append(MutableComponent.literal(usage).formatted(ChatFormatting.GREEN));
            }
        }

        if (usagesText.getString().isEmpty()) {
            usagesText.append(MutableComponent.literal("\n " + cmd).formatted(ChatFormatting.GREEN));
        }

        return MutableComponent.literal("\n Usage:").formatted(ChatFormatting.GRAY).append(usagesText);
    }
}
