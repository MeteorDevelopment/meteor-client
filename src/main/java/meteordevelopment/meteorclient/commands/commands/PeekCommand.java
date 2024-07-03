/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class PeekCommand extends Command {
    private static final ItemStack[] ITEMS = new ItemStack[27];
    private static final SimpleCommandExceptionType CANT_PEEK = new SimpleCommandExceptionType(Text.literal("You must be holding a storage block or looking at an item frame."));

    public PeekCommand() {
        super("peek", "Lets you see what's inside storage block items.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            if (Utils.openContainer(mc.player.getMainHandStack(), ITEMS, true)) return SINGLE_SUCCESS;
            else if (Utils.openContainer(mc.player.getOffHandStack(), ITEMS, true)) return SINGLE_SUCCESS;
            else if (mc.targetedEntity instanceof ItemFrameEntity &&
                Utils.openContainer(((ItemFrameEntity) mc.targetedEntity).getHeldItemStack(), ITEMS, true)
            ) return SINGLE_SUCCESS;
            else throw CANT_PEEK.create();
        });
    }
}
