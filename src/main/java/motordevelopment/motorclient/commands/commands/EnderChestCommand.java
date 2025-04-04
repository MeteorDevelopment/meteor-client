/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import motordevelopment.motorclient.commands.Command;
import motordevelopment.motorclient.utils.Utils;
import net.minecraft.command.CommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class EnderChestCommand extends Command {
    public EnderChestCommand() {
        super("ender-chest", "Allows you to preview memory of your ender chest.", "ec", "echest");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            Utils.openContainer(Items.ENDER_CHEST.getDefaultStack(), new ItemStack[27], true);
            return SINGLE_SUCCESS;
        });
    }
}
