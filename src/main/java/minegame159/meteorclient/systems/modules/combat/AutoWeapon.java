/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.combat;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.entity.player.AttackEntityEvent;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.player.InvUtils;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityGroup;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;

public class AutoWeapon extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Weapon> weapon = sgGeneral.add(new EnumSetting.Builder<Weapon>()
        .name("weapon")
        .description("What type of weapon to use.")
        .defaultValue(Weapon.Sword)
        .build()
    );

    private final Setting<Integer> threshold = sgGeneral.add(new IntSetting.Builder()
        .name("threshold")
        .description("If the non-preferred weapon produces this much damage this will favor it over your preferred weapon.")
        .defaultValue(4)
        .build()
    );

    private final Setting<Boolean> antiBreak = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-break")
        .description("Prevents you from breaking your weapon.")
        .defaultValue(false)
        .build()
    );

    public AutoWeapon() {
        super(Categories.Combat, "auto-weapon", "Finds the best weapon to use in your hotbar.");
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        InvUtils.swap(getBestWeapon());
    }

    private int getBestWeapon() {
        int slotS = mc.player.getInventory().selectedSlot;
        int slotA = mc.player.getInventory().selectedSlot;
        int slot = mc.player.getInventory().selectedSlot;
        double damageS = 0;
        double damageA = 0;
        double currentDamageS;
        double currentDamageA;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof SwordItem
                && (!antiBreak.get() || (mc.player.getInventory().getStack(i).getMaxDamage() - mc.player.getInventory().getStack(i).getDamage()) > 10)) {
                currentDamageS = ((SwordItem) mc.player.getInventory().getStack(i).getItem()).getMaterial().getAttackDamage() + EnchantmentHelper.getAttackDamage(mc.player.getInventory().getStack(i), EntityGroup.DEFAULT) + 2;
                if (currentDamageS > damageS) {
                    damageS = currentDamageS;
                    slotS = i;
                }
            }
        }
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof AxeItem
                && (!antiBreak.get() || (mc.player.getInventory().getStack(i).getMaxDamage() - mc.player.getInventory().getStack(i).getDamage()) > 10)) {
                currentDamageA = ((AxeItem) mc.player.getInventory().getStack(i).getItem()).getMaterial().getAttackDamage() + EnchantmentHelper.getAttackDamage(mc.player.getInventory().getStack(i), EntityGroup.DEFAULT) + 2;
                if (currentDamageA > damageA) {
                    damageA = currentDamageA;
                    slotA = i;
                }
            }
        }
        if (weapon.get() == Weapon.Sword && threshold.get() > damageA - damageS) {
            slot = slotS;
        } else if (weapon.get() == Weapon.Axe && threshold.get() > damageS - damageA) {
            slot = slotA;
        } else if (weapon.get() == Weapon.Sword && threshold.get() < damageA - damageS) {
            slot = slotA;
        } else if (weapon.get() == Weapon.Axe && threshold.get() < damageS - damageA) {
            slot = slotS;
        }
        return slot;
    }

    public enum Weapon {
        Sword,
        Axe
    }
}
