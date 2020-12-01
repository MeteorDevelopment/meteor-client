/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

//Created by squidoodly 18/04/2020

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.commands.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class Say extends Command {

    public Say() {
        super("say", "Sends messages in chat.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("message", StringArgumentType.greedyString()).executes(context -> {
            MinecraftClient.getInstance().getNetworkHandler().sendPacket(new ChatMessageC2SPacket(context.getArgument("message", String.class)));
            return SINGLE_SUCCESS;
        }));
    }
}
