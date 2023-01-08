/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.systems.commands.Commands;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.RegistryEntryArgumentType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

import java.util.function.Function;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class EnchantCommand extends Command {
    private final static SimpleCommandExceptionType NOT_IN_CREATIVE = new SimpleCommandExceptionType(Text.literal("You must be in creative mode to use this."));
    private final static SimpleCommandExceptionType NOT_HOLDING_ITEM = new SimpleCommandExceptionType(Text.literal("You need to hold some item to enchant."));

    public EnchantCommand() {
        super("enchant", "Enchants the item in your hand. REQUIRES Creative mode.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("one").then(argument("enchantment", RegistryEntryArgumentType.registryEntry(Commands.REGISTRY_ACCESS, RegistryKeys.ENCHANTMENT))
            .then(literal("level").then(argument("level", IntegerArgumentType.integer()).executes(context -> {
                one(context, enchantment -> context.getArgument("level", Integer.class));
                return SINGLE_SUCCESS;
            })))
            .then(literal("max").executes(context -> {
                one(context, Enchantment::getMaxLevel);
                return SINGLE_SUCCESS;
            }))
        ));

        builder.then(literal("all_possible")
            .then(literal("level").then(argument("level", IntegerArgumentType.integer()).executes(context -> {
                all(true, enchantment -> context.getArgument("level", Integer.class));
                return SINGLE_SUCCESS;
            })))
            .then(literal("max").executes(context -> {
                all(true, Enchantment::getMaxLevel);
                return SINGLE_SUCCESS;
            }))
        );

        builder.then(literal("all")
            .then(literal("level").then(argument("level", IntegerArgumentType.integer()).executes(context -> {
                all(false, enchantment -> context.getArgument("level", Integer.class));
                return SINGLE_SUCCESS;
            })))
            .then(literal("max").executes(context -> {
                all(false, Enchantment::getMaxLevel);
                return SINGLE_SUCCESS;
            }))
        );

        builder.then(literal("clear").executes(context -> {
            ItemStack itemStack = tryGetItemStack();
            Utils.clearEnchantments(itemStack);

            syncItem();
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("remove").then(argument("enchantment", RegistryEntryArgumentType.registryEntry(Commands.REGISTRY_ACCESS, RegistryKeys.ENCHANTMENT)).executes(context -> {
            ItemStack itemStack = tryGetItemStack();
            RegistryEntry.Reference<Enchantment> enchantment = context.getArgument("enchantment", RegistryEntry.Reference.class);
            Utils.removeEnchantment(itemStack, enchantment.value());

            syncItem();
            return SINGLE_SUCCESS;
        })));
    }

    private void one(CommandContext<CommandSource> context, Function<Enchantment, Integer> level) throws CommandSyntaxException {
        ItemStack itemStack = tryGetItemStack();

        RegistryEntry.Reference<Enchantment> enchantment = context.getArgument("enchantment", RegistryEntry.Reference.class);
        Utils.addEnchantment(itemStack, enchantment.value(), level.apply(enchantment.value()));

        syncItem();
    }

    private void all(boolean onlyPossible, Function<Enchantment, Integer> level) throws CommandSyntaxException {
        ItemStack itemStack = tryGetItemStack();

        for (Enchantment enchantment : Registries.ENCHANTMENT) {
            if (!onlyPossible || enchantment.isAcceptableItem(itemStack)) {
                Utils.addEnchantment(itemStack, enchantment, level.apply(enchantment));
            }
        }

        syncItem();
    }

    private void syncItem() {
        mc.setScreen(new InventoryScreen(mc.player));
        mc.setScreen(null);
    }

    private ItemStack tryGetItemStack() throws CommandSyntaxException {
        if (!mc.player.isCreative()) throw NOT_IN_CREATIVE.create();

        ItemStack itemStack = getItemStack();
        if (itemStack == null) throw NOT_HOLDING_ITEM.create();

        return itemStack;
    }

    private ItemStack getItemStack() {
        ItemStack itemStack = mc.player.getMainHandStack();
        if (itemStack == null) itemStack = mc.player.getOffHandStack();
        return itemStack.isEmpty() ? null : itemStack;
    }
}
