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
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class DropCommand extends Command {
    private static final SimpleCommandExceptionType NOT_SPECTATOR = new SimpleCommandExceptionType(Component.literal("Can't drop items while in spectator."));
    private static final SimpleCommandExceptionType NO_SUCH_ITEM = new SimpleCommandExceptionType(Component.literal("Could not find an item with that name!"));

    public DropCommand() {
        super("drop", "Automatically drops specified items.");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        // Main Hand
        builder.then(literal("hand").executes(context -> drop(player -> player.drop(true))));

        // Offhand
        builder.then(literal("offhand").executes(context -> drop(player -> InvUtils.drop().slotOffhand())));

        // Hotbar
        builder.then(literal("hotbar").executes(context -> drop(player -> {
            for (int i = 0; i < 9; i++) {
                InvUtils.drop().slotHotbar(i);
            }
        })));

        // Main Inv
        builder.then(literal("inventory").executes(context -> drop(player -> {
            for (int i = 0; i < 27; i++) {
                InvUtils.drop().slotMain(i);
            }
        })));

        // Hotbar and main inv
        builder.then(literal("all").executes(context -> drop(player -> {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                InvUtils.drop().slot(i);
            }
            if (!mc.player.getOffhandItem().isEmpty()) InvUtils.drop().slotOffhand();
        })));

        // Armor
        builder.then(literal("armor").executes(context -> drop(player -> {
            for (EquipmentSlot equipmentSlot : EquipmentSlotGroup.ARMOR) {
                if (equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                    InvUtils.drop().slotArmor(equipmentSlot.getIndex());
                }
            }
        })));

        // Specific item
        builder.then(argument("item", ItemArgument.item(REGISTRY_ACCESS))
            .executes(context -> drop(player -> {
                dropItem(player, context, Integer.MAX_VALUE);
            }))
            .then(argument("amount", IntegerArgumentType.integer(1))
                .executes(context -> drop(player -> {
                    int amount = IntegerArgumentType.getInteger(context, "amount");
                    dropItem(player, context, amount);
                })))
        );
    }

    private void dropItem(LocalPlayer player, CommandContext<SharedSuggestionProvider> context, int amount) throws CommandSyntaxException {
        ItemStack stack = ItemArgument.getItem(context, "item").createItemStack(1, false);
        if (stack == null || stack.getItem() == Items.AIR) throw NO_SUCH_ITEM.create();

        for (int i = 0; i < player.getInventory().getContainerSize() && amount > 0; i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (invStack.isEmpty() || stack.getItem() != invStack.getItem()) continue;

            int dropCount = Math.min(amount, invStack.getCount());

            if (dropCount == invStack.getCount()) {
                InvUtils.drop().slot(i);
            } else {
                for (int j = 0; j < dropCount; j++) {
                    InvUtils.dropOne().slot(i);
                }
            }

            amount -= dropCount;
        }
    }

    private int drop(PlayerConsumer consumer) throws CommandSyntaxException {
        if (mc.player.isSpectator()) throw NOT_SPECTATOR.create();
        consumer.accept(mc.player);
        return SINGLE_SUCCESS;
    }

    // Separate interface so exceptions can be thrown from it (which is not the case for Consumer)
    @FunctionalInterface
    private interface PlayerConsumer {
        void accept(LocalPlayer player) throws CommandSyntaxException;
    }
}
