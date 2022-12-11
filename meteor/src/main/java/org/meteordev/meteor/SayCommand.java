/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package org.meteordev.meteor;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import org.meteordev.meteor.api.commands.AbstractCommand;

public class SayCommand extends AbstractCommand {
    public SayCommand() {
        super("say", "Sends a message in the chat.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("message", StringArgumentType.greedyString()).executes(context -> {
            String message = context.getArgument("message", String.class);

            MinecraftClient.getInstance().getNetworkHandler().sendChatMessage(message);

            return SUCCESS;
        }));
    }
}
