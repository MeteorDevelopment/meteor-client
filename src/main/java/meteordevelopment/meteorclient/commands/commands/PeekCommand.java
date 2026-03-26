/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;

public class PeekCommand extends Command {
    private static final ItemStack[] ITEMS = new ItemStack[27];
    private static final SimpleCommandExceptionType CANT_PEEK = new SimpleCommandExceptionType(Component.literal("You must be holding a storage block or looking at an item frame."));

    public PeekCommand() {
        super("peek", "Lets you see what's inside storage block items.");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.executes(context -> {
            if (Utils.openContainer(mc.player.getMainHandItem(), ITEMS, true)) return SINGLE_SUCCESS;
            else if (Utils.openContainer(mc.player.getOffhandItem(), ITEMS, true)) return SINGLE_SUCCESS;
            else if (mc.crosshairPickEntity instanceof ItemFrame &&
                Utils.openContainer(((ItemFrame) mc.crosshairPickEntity).getItem(), ITEMS, true)
            ) return SINGLE_SUCCESS;
            else throw CANT_PEEK.create();
        });
    }
}
