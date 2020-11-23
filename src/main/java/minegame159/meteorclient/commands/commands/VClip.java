/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.utils.Chat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public class VClip extends Command {
    public VClip() {
        super("vclip", "Lets you clip through blocks vertically.");
    }

    @Override
    public void run(String[] args) {
        if (args.length == 0) {
            sendErrorMsg();
            return;
        }

        try {
            double blocks = Double.parseDouble(args[0]);
            ClientPlayerEntity player = MinecraftClient.getInstance().player;

            player.updatePosition(player.getX(), player.getY() + blocks, player.getZ());
        } catch (NumberFormatException ignored) {
            sendErrorMsg();
        }
    }

    private void sendErrorMsg() {
        Chat.error("Specify a number.");
    }
}
