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
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

public class DropCommand extends Command {
    private static final SimpleCommandExceptionType NOT_SPECTATOR = new SimpleCommandExceptionType(Text.literal("Can't drop items while in spectator."));
    private static final SimpleCommandExceptionType NO_SUCH_ITEM = new SimpleCommandExceptionType(Text.literal("Could not find an item with that name!"));

    public DropCommand() {
        super("drop", "Automatically drops specified items.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        // Main Hand
        builder.then(literal("hand").executes(context -> drop(player -> player.dropSelectedItem(true))));

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
            for (int i = 0; i < player.getInventory().size(); i++) {
                InvUtils.drop().slot(i);
            }
            if (!mc.player.getOffHandStack().isEmpty()) InvUtils.drop().slotOffhand();
        })));

        // Armor
        builder.then(literal("armor").executes(context -> drop(player -> {
            for (EquipmentSlot equipmentSlot : AttributeModifierSlot.ARMOR) {
                if (equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                    InvUtils.drop().slotArmor(equipmentSlot.getEntitySlotId());
                }
            }
        })));

        // Specific item
        builder.then(argument("item", ItemStackArgumentType.itemStack(REGISTRY_ACCESS))
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

    private void dropItem(ClientPlayerEntity player, CommandContext<CommandSource> context, int amount) throws CommandSyntaxException {
        ItemStack stack = ItemStackArgumentType.getItemStackArgument(context, "item").createStack(1, false);
        if (stack == null || stack.getItem() == Items.AIR) throw NO_SUCH_ITEM.create();

        for (int i = 0; i < player.getInventory().size() && amount > 0; i++) {
            ItemStack invStack = player.getInventory().getStack(i);
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
        void accept(ClientPlayerEntity player) throws CommandSyntaxException;
    }
}
