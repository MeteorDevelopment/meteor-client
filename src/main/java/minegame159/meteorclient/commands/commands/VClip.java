/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.utils.Chat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class VClip extends Command {
    public VClip() {
        super("vclip", "Lets you clip through blocks vertically.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("blocks", DoubleArgumentType.doubleArg()).executes(context -> {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            assert player != null;

            double blocks = context.getArgument("blocks", Double.class);
            player.updatePosition(player.getX(), player.getY() + blocks, player.getZ());

            return SINGLE_SUCCESS;
        }));
    }

    private void sendErrorMsg() {
        Chat.error("Specify a number.");
    }
}
