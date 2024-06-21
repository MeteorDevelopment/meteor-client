/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.world.GameMode;

public class GamemodeCommand extends Command {
    public GamemodeCommand() {
        super("gamemode", "Changes your gamemode client-side.", "gm");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        for (GameMode gameMode : GameMode.values()) {
            builder.then(literal(gameMode.getName()).executes(context -> {
                mc.interactionManager.setGameMode(gameMode);
                return SINGLE_SUCCESS;
            }));
        }
    }
}
