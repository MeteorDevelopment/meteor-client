/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;

public class DisconnectCommand extends Command {
    public DisconnectCommand() {
        super("disconnect", "Disconnect from the server", "dc");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.executes(context -> {
            mc.player.connection.handleDisconnect(new ClientboundDisconnectPacket(Component.literal("%s[%sDisconnectCommand%s] Disconnected by user.".formatted(ChatFormatting.GRAY, ChatFormatting.BLUE, ChatFormatting.GRAY))));
            return SINGLE_SUCCESS;
        });

        builder.then(argument("reason", StringArgumentType.greedyString()).executes(context -> {
            mc.player.connection.handleDisconnect(new ClientboundDisconnectPacket(Component.literal(StringArgumentType.getString(context, "reason"))));
            return SINGLE_SUCCESS;
        }));
    }
}
