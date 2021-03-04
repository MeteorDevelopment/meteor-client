/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.world.GameMode;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class Gamemode extends Command {
    public Gamemode() {
        super("gamemode", "Changes your gamemode client-side.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        for (GameMode gameMode : GameMode.values()) {
            if (gameMode == GameMode.NOT_SET) continue;

            builder.then(literal(gameMode.getName()).executes(context -> {
                mc.player.setGameMode(gameMode);
                mc.interactionManager.setGameMode(gameMode);

                return SINGLE_SUCCESS;
            }));
        }
    }
}
