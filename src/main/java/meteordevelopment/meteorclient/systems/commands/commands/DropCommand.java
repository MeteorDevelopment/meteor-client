/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.systems.commands.Commands;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

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
            for (int i = 9; i < player.getInventory().main.size(); i++) {
                InvUtils.drop().slotMain(i - 9);
            }
        })));

        // Hotbar and main inv
        builder.then(literal("all").executes(context -> drop(player -> {
                    for (int i = 0; i < player.getInventory().size(); i++) {
                        InvUtils.drop().slot(i);
                    }
                })));

        // Armor
        builder.then(literal("armor").executes(context -> drop(player -> {
                    for (int i = 0; i < player.getInventory().armor.size(); i++) {
                        InvUtils.drop().slotArmor(i);
                    }
                })));

        // Specific item
        builder.then(argument("item", ItemStackArgumentType.itemStack(Commands.REGISTRY_ACCESS)).executes(context -> drop(player -> {
            ItemStack stack = ItemStackArgumentType.getItemStackArgument(context, "item").createStack(1, false);

            if (stack == null || stack.getItem() == Items.AIR) throw NO_SUCH_ITEM.create();

            for (int i = 0; i < player.getInventory().size(); i++) {
                if (stack.getItem() == player.getInventory().getStack(i).getItem()) {
                    InvUtils.drop().slot(i);
                }
            }
        })));
    }

    private int drop(PlayerConsumer consumer) throws CommandSyntaxException {
        ClientPlayerEntity player = mc.player;
        assert player != null;

        if (player.isSpectator()) throw NOT_SPECTATOR.create();

        consumer.accept(player);
        return SINGLE_SUCCESS;
    }

    // Separate interface so exceptions can be thrown from it (which is not the case for Consumer)
    @FunctionalInterface
    private interface PlayerConsumer {
        void accept(ClientPlayerEntity player) throws CommandSyntaxException;
    }

}
