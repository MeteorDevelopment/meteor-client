/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityGroup;
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

    private final Setting<Boolean> switchSecondary = sgGeneral.add(new BoolSetting.Builder()
        .name("switch to secondary weapon on one-hit")
        .description("Switches to secondary weapon when the victim's health goes below the desired number")
        .defaultValue(false)
        .build()
    );
    private final Setting<Weapon2> weapon2 = sgGeneral.add(new EnumSetting.Builder<Weapon2>()
        .name("secondary weapon")
        .description("what weapon to switch to on the desired value")
        .defaultValue(Weapon2.Sword)
        .visible(switchSecondary::get)
        .build()
    );
    private final Setting<Double> healthThreshold = sgGeneral.add(new DoubleSetting.Builder()
        .name("health threshold")
        .description("When to trigger the secondary weapon based on victim's health")
        .defaultValue(5)
        .sliderRange(1, 36)
        .visible(switchSecondary::get)
        .build()
    );

    public int bsfr(LivingEntity entity) {
        return (int) entity.getHealth();
    }

    public AutoWeapon() {
        super(Categories.Combat, "auto-weapon", "Finds the best weapon to use in your hotbar.");
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (switchSecondary.get() && bsfr((LivingEntity) event.entity) < healthThreshold.get()) {
            InvUtils.swap(getSecondaryWeapon(EntityUtils.getGroup(event.entity), (int) (bsfr((LivingEntity) event.entity))), false);
        } else
        InvUtils.swap(getBestWeapon(EntityUtils.getGroup(event.entity)), false);
    }
    private int getSecondaryWeapon(EntityGroup group, int entityHealth) {
        int slotS = mc.player.getInventory().selectedSlot;
        int slotA = mc.player.getInventory().selectedSlot;
        double speedS = 0;
        double speedA = 0;
        double currentSpeedS= 0;
        double currentSpeedA = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof SwordItem swordItem
                && (!antiBreak.get() || (stack.getMaxDamage() - stack.getDamage()) > 10)) {
                currentSpeedS = swordItem.getMaterial().getAttackDamage() + EnchantmentHelper.getAttackDamage(stack, group) + 2;
                if (currentSpeedS > speedS) {
                    speedS = currentSpeedS;
                    slotS = i;
                }
            } else if (stack.getItem() instanceof AxeItem axeItem
                && (!antiBreak.get() || (stack.getMaxDamage() - stack.getDamage()) > 10)) {
                currentSpeedA = axeItem.getMaterial().getAttackDamage() + EnchantmentHelper.getAttackDamage(stack, group) + 2;
                if (currentSpeedA > speedA) {
                    speedA = currentSpeedA;
                    slotA = i;
                }
            }
        }
        if (weapon2.get() == Weapon2.Sword && threshold.get() > speedA - speedS && healthThreshold.get() > entityHealth) {
            return slotS;
        }
        else if (weapon2.get() == Weapon2.Axe && threshold.get() > speedS - speedA && healthThreshold.get() > entityHealth) {
            return slotA;
        }
        else if (weapon2.get() == Weapon2.Sword && threshold.get() < speedA - speedS && healthThreshold.get() > entityHealth) {
            return slotA;
        }
        else if (weapon2.get() == Weapon2.Axe && threshold.get() < speedS - speedA && healthThreshold.get() > entityHealth) {
            return slotS;
        }
        else {
            return mc.player.getInventory().selectedSlot;
        }
    }

    private int getBestWeapon(EntityGroup group) {
        int slotS = mc.player.getInventory().selectedSlot;
        int slotA = mc.player.getInventory().selectedSlot;
        double damageS = 0;
        double damageA = 0;
        double currentDamageS;
        double currentDamageA;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof SwordItem swordItem
                && (!antiBreak.get() || (stack.getMaxDamage() - stack.getDamage()) > 10)) {
                currentDamageS = swordItem.getMaterial().getAttackDamage() + EnchantmentHelper.getAttackDamage(stack, group) + 2;
                if (currentDamageS > damageS) {
                    damageS = currentDamageS;
                    slotS = i;
                }
            } else if (stack.getItem() instanceof AxeItem axeItem
                && (!antiBreak.get() || (stack.getMaxDamage() - stack.getDamage()) > 10)) {
                currentDamageA = axeItem.getMaterial().getAttackDamage() + EnchantmentHelper.getAttackDamage(stack, group) + 2;
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
    public enum Weapon2 {

        Sword,
        Axe
    }
}
