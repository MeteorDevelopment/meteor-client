/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;
//Created by squidoodly 27/05/2020

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class Enchant extends Command {
    private final static SimpleCommandExceptionType NOT_IN_CREATIVE =
            new SimpleCommandExceptionType(new LiteralText("You must be in creative mode to use this."));

    public Enchant() {
        super("enchant", "Enchants the currently held item with almost every enchantment (must be in creative)");
    }

    private final MinecraftClient mc = MinecraftClient.getInstance();

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("level", IntegerArgumentType.integer())
                .executes(context -> {
                    assert mc.player != null;

                    if (!mc.player.isCreative()) {
                        throw NOT_IN_CREATIVE.create();
                    }

                    int level = context.getArgument("level", Integer.class);
                    addEnchantments(level);

                    return SINGLE_SUCCESS;
                }));
    }

    private void addEnchantments(int level) {
        assert mc.player != null;
        ItemStack stack = mc.player.getMainHandStack();

        Utils.addEnchantment(stack, Enchantments.AQUA_AFFINITY, level);
        Utils.addEnchantment(stack, Enchantments.BANE_OF_ARTHROPODS, level);
        Utils.addEnchantment(stack, Enchantments.BLAST_PROTECTION, level);
        Utils.addEnchantment(stack, Enchantments.CHANNELING, level);
        Utils.addEnchantment(stack, Enchantments.DEPTH_STRIDER, level);
        Utils.addEnchantment(stack, Enchantments.EFFICIENCY, level);
        Utils.addEnchantment(stack, Enchantments.FEATHER_FALLING, level);
        Utils.addEnchantment(stack, Enchantments.FIRE_ASPECT, level);
        Utils.addEnchantment(stack, Enchantments.FIRE_PROTECTION, level);
        Utils.addEnchantment(stack, Enchantments.FLAME, level);
        Utils.addEnchantment(stack, Enchantments.FORTUNE, level);
        Utils.addEnchantment(stack, Enchantments.FROST_WALKER, level);
        Utils.addEnchantment(stack, Enchantments.IMPALING, level);
        Utils.addEnchantment(stack, Enchantments.INFINITY, level);
        Utils.addEnchantment(stack, Enchantments.LOOTING, level);
        Utils.addEnchantment(stack, Enchantments.LOYALTY, level);
        Utils.addEnchantment(stack, Enchantments.LUCK_OF_THE_SEA, level);
        Utils.addEnchantment(stack, Enchantments.LURE, level);
        Utils.addEnchantment(stack, Enchantments.MENDING, level);
        Utils.addEnchantment(stack, Enchantments.PIERCING, level);
        Utils.addEnchantment(stack, Enchantments.POWER, level);
        Utils.addEnchantment(stack, Enchantments.PROJECTILE_PROTECTION, level);
        Utils.addEnchantment(stack, Enchantments.PROTECTION, level);
        Utils.addEnchantment(stack, Enchantments.PUNCH, level);
        Utils.addEnchantment(stack, Enchantments.QUICK_CHARGE, level);
        Utils.addEnchantment(stack, Enchantments.RESPIRATION, level);
        Utils.addEnchantment(stack, Enchantments.SHARPNESS, level);
        Utils.addEnchantment(stack, Enchantments.SMITE, level);
        Utils.addEnchantment(stack, Enchantments.SWEEPING, level);
        Utils.addEnchantment(stack, Enchantments.SHARPNESS, level);
        Utils.addEnchantment(stack, Enchantments.THORNS, level);
        Utils.addEnchantment(stack, Enchantments.UNBREAKING, level);
        Utils.addEnchantment(stack, Enchantments.VANISHING_CURSE, level);
    }
}
