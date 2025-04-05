/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.ItemSlotArgumentType;
import meteordevelopment.meteorclient.utils.commands.CreativeCommandHelper;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class GiveCommand extends Command {
    private final static SimpleCommandExceptionType NO_FREE_SPACE = new SimpleCommandExceptionType(Text.literal("No free space in hotbar."));

    public GiveCommand() {
        super("give", "Gives you any item.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("item", ItemStackArgumentType.itemStack(REGISTRY_ACCESS))
            .executes(context -> executeGive(context, 1, findEmptySlot()))
            .then(literal("slot").then(argument("slot", ItemSlotArgumentType.modifiableSlot()).executes(context ->
                executeGive(context, 1, ItemSlotArgumentType.getItemSlot(context))
            )))
            .then(argument("count", IntegerArgumentType.integer(1, 99))
                .executes(context -> executeGive(context, IntegerArgumentType.getInteger(context, "count"), findEmptySlot()))
                .then(literal("slot").then(argument("slot", ItemSlotArgumentType.modifiableSlot()).executes(context ->
                    executeGive(context, IntegerArgumentType.getInteger(context, "count"), ItemSlotArgumentType.getItemSlot(context))
                )))
            )
        );
    }

    private int findEmptySlot() throws CommandSyntaxException {
        FindItemResult fir = InvUtils.findInHotbar(ItemStack::isEmpty);
        if (!fir.found()) throw NO_FREE_SPACE.create();
        return fir.slot();
    }

    private <S> int executeGive(CommandContext<S> context, int count, int slot) throws CommandSyntaxException {
        ItemStack stack = ItemStackArgumentType.getItemStackArgument(context, "item").createStack(count, false);
        CreativeCommandHelper.setStack(stack, slot);

        return SINGLE_SUCCESS;
    }
}
