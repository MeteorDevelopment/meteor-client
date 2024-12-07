/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.EChestMemory;
import net.minecraft.command.CommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class EnderChestCommand extends Command {
    private static final SimpleCommandExceptionType NO_MEMORY_EXCEPTION = new SimpleCommandExceptionType(new LiteralMessage("No saved ender chest memory."));

    public EnderChestCommand() {
        super("ender-chest", "Allows you to preview memory of your ender chest.", "ec", "echest");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            if (!EChestMemory.isKnown()) {
                throw NO_MEMORY_EXCEPTION.create();
            }

            Utils.openContainer(Items.ENDER_CHEST.getDefaultStack(), new ItemStack[27], true);
            return SINGLE_SUCCESS;
        });
    }
}
