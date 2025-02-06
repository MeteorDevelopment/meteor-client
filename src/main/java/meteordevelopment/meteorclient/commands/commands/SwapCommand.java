/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.command.CommandSource;

public class SwapCommand extends Command {
    public SwapCommand() {
        super("swap", "Swaps to a hotbar slot");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("slot", IntegerArgumentType.integer(1, 9)).executes(context -> {
            int slot = IntegerArgumentType.getInteger(context, "slot") - 1;
            InvUtils.swap(slot, false);
            return SINGLE_SUCCESS;
        }));

    }
}
