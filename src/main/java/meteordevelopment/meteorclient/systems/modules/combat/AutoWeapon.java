/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.MaceItem;
import net.minecraft.item.TridentItem;
import java.lang.Math;

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
        if (event.entity instanceof LivingEntity livingEntity) {
            InvUtils.swap(getBestWeapon(livingEntity), false);
        }
    }

    private int getBestWeapon(LivingEntity target) {
        int slotS = mc.player.getInventory().selectedSlot;
        int slotA = mc.player.getInventory().selectedSlot;
        int slotT = mc.player.getInventory().selectedSlot;
        int slotM = mc.player.getInventory().selectedSlot;
        double damageS = 0;
        double damageA = 0;
        double damageT = 0;
        double damageM = 0;
        double currentDamageS;
        double currentDamageA;
        double currentDamageT;
        double currentDamageM;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof SwordItem
                    && (!antiBreak.get() || (stack.getMaxDamage() - stack.getDamage()) > 10)) {
                currentDamageS = DamageUtils.getAttackDamage(mc.player, target, stack);
                if (currentDamageS > damageS) {
                    damageS = currentDamageS;
                    slotS = i;
                }
            } else if (stack.getItem() instanceof AxeItem
                    && (!antiBreak.get() || (stack.getMaxDamage() - stack.getDamage()) > 10)) {
                currentDamageA = DamageUtils.getAttackDamage(mc.player, target, stack);
                if (currentDamageA > damageA) {
                    damageA = currentDamageA;
                    slotA = i;
                }
            } else if (stack.getItem() instanceof TridentItem
                    && (!antiBreak.get() || (stack.getMaxDamage() - stack.getDamage()) > 10)) {
                currentDamageT = DamageUtils.getAttackDamage(mc.player, target, stack);
                if (currentDamageT > damageT) {
                    damageT = currentDamageT;
                    slotT = i;
                }
            } else if (stack.getItem() instanceof MaceItem
                    && (!antiBreak.get() || (stack.getMaxDamage() - stack.getDamage()) > 10)) {
                currentDamageM = DamageUtils.getAttackDamage(mc.player, target, stack);
                if (currentDamageM > damageM) {
                    damageM = currentDamageM;
                    slotM = i;
                }
            }
        }
        if (weapon.get() != Weapon.Sword) damageS -= 4;
        if (weapon.get() != Weapon.Axe) damageA -= 4;
        if (weapon.get() != Weapon.Trident) damageT -= 4;
        if (weapon.get() != Weapon.Mace) damageM -= 4;
        double max = Math.max(Math.max(damageS, damageA), Math.max(damageT, damageM));
            if (max == damageS) return slotS;
            else if (max == damageA) return slotA;
            else if (max == damageT) return slotT;
            else if (max == damageM) return slotT;
            else return mc.player.getInventory().selectedSlot;
    }

    public enum Weapon {
        Sword,
        Axe,
        Trident,
        Mace
    }
}
