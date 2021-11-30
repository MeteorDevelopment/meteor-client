/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AutoExp extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> durabilityThreshold = sgGeneral.add(new DoubleSetting.Builder()
        .name("threshold")
        .description("The durability percentage at which we should start repairing.")
        .defaultValue(30)
        .build()
    );

    public AutoExp() {
        super(Categories.Combat, "auto-exp", "Automatically throws XP bottles in your hotbar.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        boolean shouldThrow = false;

        for (ItemStack itemStack : mc.player.getInventory().armor) {
            if (itemStack.isEmpty()) continue;
            if (EnchantmentHelper.getLevel(Enchantments.MENDING, itemStack) < 1) continue;

            // get item durability as a percentage
            double durability = ((itemStack.getDamage() - itemStack.getMaxDamage()) / (double) itemStack.getMaxDamage()) * 100;

            if (durability < durabilityThreshold.get()) {
                shouldThrow = true;
                break;
            }
        }

        if (shouldThrow) {
            FindItemResult exp = InvUtils.findInHotbar(Items.EXPERIENCE_BOTTLE);

            if (exp.found()) {
                Rotations.rotate(mc.player.getYaw(), 90, () -> {
                    if (exp.getHand() != null) {
                        mc.interactionManager.interactItem(mc.player, mc.world, exp.getHand());
                    }
                    else {
                        InvUtils.swap(exp.getSlot(), true);
                        mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
                        InvUtils.swapBack();
                    }
                });
            }
        }
    }
}
