/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.chat.Component;

public class GiveCommand extends Command {
    private final static SimpleCommandExceptionType NOT_IN_CREATIVE = new SimpleCommandExceptionType(Component.literal("You must be in creative mode to use this."));
    private final static SimpleCommandExceptionType NO_SPACE = new SimpleCommandExceptionType(Component.literal("No space in hotbar."));

    public GiveCommand() {
        super("give", "Gives you any item.");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.then(argument("item", ItemArgument.itemStack(REGISTRY_ACCESS)).executes(context -> {
            if (!mc.player.getAbilities().creativeMode) throw NOT_IN_CREATIVE.create();

            ItemStack item = ItemArgument.getItemStackArgument(context, "item").createStack(1, false);
            giveItem(item);

            return SINGLE_SUCCESS;
        }).then(argument("number", IntegerArgumentType.integer(1, 99)).executes(context -> {
            if (!mc.player.getAbilities().creativeMode) throw NOT_IN_CREATIVE.create();

            ItemStack item = ItemArgument.getItemStackArgument(context, "item").createStack(IntegerArgumentType.getInteger(context, "number"), true);
            giveItem(item);

            return SINGLE_SUCCESS;
        })));
    }

    private void giveItem(ItemStack item) throws CommandSyntaxException {
        FindItemResult fir = InvUtils.find(ItemStack::isEmpty, 0, 8);
        if (!fir.found()) throw NO_SPACE.create();

        mc.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(36 + fir.slot(), item));
        mc.player.playerScreenHandler.getSlot(36 + fir.slot()).setStack(item);
    }
}
