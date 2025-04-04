/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.systems.modules.combat;

import motordevelopment.motorclient.events.entity.player.AttackEntityEvent;
import motordevelopment.motorclient.settings.*;
import motordevelopment.motorclient.systems.modules.Categories;
import motordevelopment.motorclient.systems.modules.Module;
import motordevelopment.motorclient.utils.entity.DamageUtils;
import motordevelopment.motorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
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
        if (event.entity instanceof LivingEntity livingEntity) {
            InvUtils.swap(getBestWeapon(livingEntity), false);
        }
    }

    private int getBestWeapon(LivingEntity target) {
        int slotS = mc.player.getInventory().selectedSlot;
        int slotA = mc.player.getInventory().selectedSlot;
        double damageS = 0;
        double damageA = 0;
        double currentDamageS;
        double currentDamageA;
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
            }
        }
        if (weapon.get() == Weapon.Sword && threshold.get() > damageA - damageS) return slotS;
        else if (weapon.get() == Weapon.Axe && threshold.get() > damageS - damageA) return slotA;
        else if (weapon.get() == Weapon.Sword && threshold.get() < damageA - damageS) return slotA;
        else if (weapon.get() == Weapon.Axe && threshold.get() < damageS - damageA) return slotS;
        else return mc.player.getInventory().selectedSlot;
    }

    public enum Weapon {
        Sword,
        Axe
    }
}
