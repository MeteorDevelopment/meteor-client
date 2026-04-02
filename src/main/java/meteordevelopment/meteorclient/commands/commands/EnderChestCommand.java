/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class EnderChestCommand extends Command {
    public EnderChestCommand() {
        super("ender-chest", "Allows you to preview memory of your ender chest.", "ec", "echest");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.executes(context -> {
            Utils.openContainer(Items.ENDER_CHEST.getDefaultInstance(), new ItemStack[27], true);
            return SINGLE_SUCCESS;
        });
    }
}
