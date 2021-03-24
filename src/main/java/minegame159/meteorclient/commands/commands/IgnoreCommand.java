/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

//Created by squidoodly 01/07/2020

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.systems.Ignores;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class IgnoreCommand extends Command {
    public IgnoreCommand() {
        super("ignore", "Lets you ignore messages from specific players.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("username", StringArgumentType.string()).executes(context -> {
            String username = context.getArgument("username", String.class);

            if (Ignores.get().remove(username)) {
                ChatUtils.prefixInfo("Ignore","Removed (highlight)%s (default)from list of ignored people.", username);
            } else {
                Ignores.get().add(username);
                ChatUtils.prefixInfo("Ignore","Added (highlight)%s (default)to list of ignored people.", username);
            }

            return SINGLE_SUCCESS;
        })).executes(context -> {
            ChatUtils.prefixInfo("Ignore","Ignoring (highlight)%d (default)people:", Ignores.get().count());
            for (String player : Ignores.get()) {
                ChatUtils.info("- (highlight)%s", player);
            }

            return SINGLE_SUCCESS;
        });
    }
}
