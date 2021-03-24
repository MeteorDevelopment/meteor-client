/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.commands.arguments.PlayerArgumentType;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class InventoryCommand extends Command {
    public InventoryCommand() {
        super("inventory", "Allows you to see parts of another player's inventory.", "inv");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("name", PlayerArgumentType.player()).executes(context -> {
            PlayerEntity playerEntity = context.getArgument("name", PlayerEntity.class);
            MeteorClient.INSTANCE.screenToOpen = new InventoryScreen(playerEntity);
            return SINGLE_SUCCESS;
        }));

    }

}
