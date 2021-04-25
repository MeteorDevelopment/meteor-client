/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.combat;

//Updated by squidoodly 15/06/2020

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.entity.player.AttackEntityEvent;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityGroup;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;

public class AutoWeapon extends Module {
    public enum Weapon{
        Sword,
        Axe
    }

    public AutoWeapon(){
        super(Categories.Combat, "auto-weapon", "Finds the best weapon to use in your hotbar.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Weapon> weapon = sgGeneral.add(new EnumSetting.Builder<Weapon>()
            .name("Weapon")
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

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        mc.player.inventory.selectedSlot = getBestWeapon();
    }

    private int getBestWeapon(){
        int slotS = mc.player.inventory.selectedSlot;
        int slotA = mc.player.inventory.selectedSlot;
        int slot = mc.player.inventory.selectedSlot;
        double damageS = 0;
        double damageA = 0;
        double currentDamageS;
        double currentDamageA;
        for(int i = 0; i < 9; i++){
            if(mc.player.inventory.getStack(i).getItem() instanceof SwordItem
                    && (!antiBreak.get() || (mc.player.inventory.getStack(i).getMaxDamage() - mc.player.inventory.getStack(i).getDamage()) > 10)){
                currentDamageS = ((SwordItem) mc.player.inventory.getStack(i).getItem()).getMaterial().getAttackDamage() + EnchantmentHelper.getAttackDamage(mc.player.inventory.getStack(i), EntityGroup.DEFAULT) + 2;
                if(currentDamageS > damageS){
                    damageS = currentDamageS;
                    slotS = i;
                }
            }
        }
        for(int i = 0; i < 9; i++){
            if(mc.player.inventory.getStack(i).getItem() instanceof AxeItem
                    && (!antiBreak.get() || (mc.player.inventory.getStack(i).getMaxDamage() - mc.player.inventory.getStack(i).getDamage()) > 10)){
                currentDamageA = ((AxeItem) mc.player.inventory.getStack(i).getItem()).getMaterial().getAttackDamage() + EnchantmentHelper.getAttackDamage(mc.player.inventory.getStack(i), EntityGroup.DEFAULT) + 2;
                if(currentDamageA > damageA){
                    damageA = currentDamageA;
                    slotA = i;
                }
            }
        }
        if(weapon.get() == Weapon.Sword && threshold.get() > damageA - damageS){
            slot = slotS;
        }else if(weapon.get() == Weapon.Axe && threshold.get() > damageS - damageA){
            slot = slotA;
        }else if(weapon.get() == Weapon.Sword && threshold.get() < damageA - damageS){
            slot = slotA;
        }else if(weapon.get() == Weapon.Axe && threshold.get() < damageS - damageA){
            slot = slotS;
        }
        return slot;
    }
}
