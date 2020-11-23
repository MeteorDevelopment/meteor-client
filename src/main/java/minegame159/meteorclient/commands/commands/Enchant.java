/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;
//Created by squidoodly 27/05/2020

import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.utils.Chat;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantments;


public class Enchant extends Command {
    public Enchant(){
        super("enchant", "Enchants the currently held item with almost every enchantment (must be in creative)");
    }

    private final MinecraftClient mc = MinecraftClient.getInstance();

    @Override
    public void run(String[] args) {
        if (args.length == 0) {
            Chat.error("Please include a level.");
            return;
        }
        if (mc.player.isCreative()) {
            try {
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.AQUA_AFFINITY, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.BANE_OF_ARTHROPODS, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.BLAST_PROTECTION, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.CHANNELING, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.DEPTH_STRIDER, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.EFFICIENCY, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.FEATHER_FALLING, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.FIRE_ASPECT, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.FIRE_PROTECTION, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.FLAME, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.FORTUNE, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.FROST_WALKER, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.IMPALING, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.INFINITY, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.LOOTING, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.LOYALTY, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.LUCK_OF_THE_SEA, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.LURE, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.MENDING, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.PIERCING, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.POWER, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.PROJECTILE_PROTECTION, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.PROTECTION, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.PUNCH, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.QUICK_CHARGE, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.RESPIRATION, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.SHARPNESS, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.SMITE, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.SWEEPING, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.SHARPNESS, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.THORNS, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.UNBREAKING, Integer.parseInt(args[0]));
                Utils.addEnchantment(mc.player.getMainHandStack(), Enchantments.VANISHING_CURSE, Integer.parseInt(args[0]));
            } catch (NumberFormatException e) {
                Chat.error("That is not a valid number. Try again.");
            }
        } else {
            Chat.error("You must be in creative mode to use this.");
        }
    }
}
