/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;

public class PeekCommand extends Command {
    private static final ItemStack[] ITEMS = new ItemStack[27];
    private static final SimpleCommandExceptionType CANT_PEEK = new SimpleCommandExceptionType(MeteorClient.translatable("command.peek.error.cant_peek"));

    public PeekCommand() {
        super("peek");
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
