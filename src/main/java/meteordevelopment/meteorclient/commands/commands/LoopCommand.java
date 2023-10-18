/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class LoopCommand extends Command {
    public LoopCommand() {
        super("loop", "Loop a meteor command x amount of times.", "lp");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder
            .then(argument("times", IntegerArgumentType.integer())
            .then(argument("command", StringArgumentType.greedyString())
            .executes((context) -> {
                int times = IntegerArgumentType.getInteger(context, "times");
                String message = StringArgumentType.getString(context, "command");

                for (int i = 0; i < times; i++) {
                    // Execute specified command
                    try {
                        Commands.dispatch(message);
                    } catch (CommandSyntaxException e) {
                        ChatUtils.error(e.getMessage());
                    }

                    MinecraftClient.getInstance().inGameHud.getChatHud().addToMessageHistory(message);
                }
                return SINGLE_SUCCESS;
            })));
    }
}
