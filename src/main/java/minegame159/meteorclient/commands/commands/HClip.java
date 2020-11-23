/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.utils.Chat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

public class HClip extends Command {
    public HClip() {
        super("hclip", "Lets your clip through blocks horizontally.");
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

            Vec3d forward = Vec3d.fromPolar(0, player.yaw).normalize();
            player.updatePosition(player.getX() + forward.x * blocks, player.getY(), player.getZ() + forward.z * blocks);
        } catch (NumberFormatException ignored) {
            sendErrorMsg();
        }
    }

    private void sendErrorMsg() {
        Chat.error("Specify a number.");
    }
}
