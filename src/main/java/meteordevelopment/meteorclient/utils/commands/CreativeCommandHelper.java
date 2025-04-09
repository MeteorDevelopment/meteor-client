/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.commands;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.item.ItemStack;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class CreativeCommandHelper {
    private static final SimpleCommandExceptionType NOT_IN_CREATIVE_EXCEPTION = new SimpleCommandExceptionType(new LiteralMessage("You must be in creative mode to run this command."));
    private static final SimpleCommandExceptionType NOT_AN_ITEM_EXCEPTION = new SimpleCommandExceptionType(new LiteralMessage("Not an item."));

    private CreativeCommandHelper() {}

    public static void assertValid(ItemStack stack) throws CommandSyntaxException {
        if (stack.isEmpty()) {
            throw NOT_AN_ITEM_EXCEPTION.create();
        }
    }

    public static void setStack(ItemStack stack) throws CommandSyntaxException {
        setStack(stack, mc.player.getInventory().selectedSlot);
    }

    public static void setStack(ItemStack stack, int slot) throws CommandSyntaxException {
        if (!mc.player.getAbilities().creativeMode) {
            throw NOT_IN_CREATIVE_EXCEPTION.create();
        }

        mc.player.getInventory().setStack(slot, stack);
        mc.interactionManager.clickCreativeStack(stack, 36 + slot);
        mc.player.playerScreenHandler.sendContentUpdates();
    }
}
