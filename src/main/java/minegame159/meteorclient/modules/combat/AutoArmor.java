/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

//Rewritten by squidoodly 25/07/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.modules.player.ChestSwap;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.DamageCalcUtils;
import minegame159.meteorclient.utils.InvUtils;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AutoArmor extends ToggleModule {
    public enum Prot{
        Protection(Enchantments.PROTECTION),
        Blast_Protection(Enchantments.BLAST_PROTECTION),
        Fire_Protection(Enchantments.FIRE_PROTECTION),
        Projectile_Protection(Enchantments.PROJECTILE_PROTECTION);

        private final Enchantment enchantment;

        Prot(Enchantment enchantment) {
            this.enchantment = enchantment;
        }
    }

    public AutoArmor(){super(Category.Combat, "auto-armor", "Manages your armor for you.");}

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Prot> mode = sgGeneral.add(new EnumSetting.Builder<Prot>()
            .name("prioritize")
            .description("Which protection to prioritize.")
            .defaultValue(Prot.Protection)
            .build()
    );

    private final Setting<Boolean> pauseInInventory = sgGeneral.add(new BoolSetting.Builder()
            .name("pause-in-inventory")
            .description("Stops moving armour when you are in an inventory")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> bProtLegs = sgGeneral.add(new BoolSetting.Builder()
            .name("blast-protection-leggings")
            .description("Prioritizes blast protection on leggings")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> preferMending = sgGeneral.add(new BoolSetting.Builder()
            .name("prefer-mending")
            .description("Prefers to equip mending than non mending.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> weight = sgGeneral.add(new IntSetting.Builder()
            .name("weight")
            .description("How preferred mending is.")
            .defaultValue(2)
            .min(1)
            .max(10)
            .sliderMax(4)
            .build()
    );

    private final Setting<List<Enchantment>> avoidEnch = sgGeneral.add(new EnchListSetting.Builder()
            .name("avoided-enchantments")
            .description("Enchantments that will only be equipped as a last resort.")
            .defaultValue(setDefaultValue())
            .build()
    );

    private final Setting<Boolean> banBinding = sgGeneral.add(new BoolSetting.Builder()
            .name("ban-binding")
            .description("Stops you from putting on any item with Curse of Binding")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> antiBreak = sgGeneral.add(new BoolSetting.Builder()
            .name("anti-break")
            .description("Tries to stop your armor getting broken.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> breakDurability = sgGeneral.add(new IntSetting.Builder()
            .name("break-durability")
            .description("The durability damaged armor is swapped.")
            .defaultValue(10)
            .max(50)
            .min(2)
            .sliderMax(20)
            .build()
    );

    private final Setting<Boolean> pause = sgGeneral.add(new BoolSetting.Builder()
            .name("pause-between-pieces")
            .description("Pauses between each individual piece being moved.(helps prevent desync)")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("The delay between pieces being moved.(helps prevent desync)")
            .defaultValue(1)
            .min(0)
            .max(20)
            .sliderMax(5)
            .build()
    );

    private final Setting<Boolean> boomSwitch = sgGeneral.add(new BoolSetting.Builder()
            .name("switch-for-explosion")
            .description("Switches to Blast Protection automatically if you're going to get hit by an explosion")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> boomDamage = sgGeneral.add(new IntSetting.Builder()
            .name("max-explosion-damage")
            .description("The maximum damage you will take before switching to Blast Protection.")
            .defaultValue(5)
            .min(1)
            .max(18)
            .sliderMax(10)
            .build()
    );

    private final Setting<Integer> switchCooldown = sgGeneral.add(new IntSetting.Builder().name("switch-cooldown")
            .description("A cooldown before switching from an automatically switched protection to your chosen protection.")
            .defaultValue(20)
            .min(0)
            .max(60)
            .sliderMax(40)
            .build()
    );

    private final Setting<Boolean> ignoreElytra = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-elytra")
            .description("Doesn't replace elytra when you have it equipped.")
            .defaultValue(false)
            .build()
    );

    private int delayLeft = delay.get();
    private boolean didSkip = false;
    private int currentBest, currentProt, currentBlast, currentFire, currentProj, currentArmour, currentUnbreaking, currentMending = 0;
    private float currentToughness = 0;

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if (mc.currentScreen != null && mc.player.inventory.size() < 44) return;
        if (mc.player.abilities.creativeMode) return;
        if (pauseInInventory.get() && mc.currentScreen instanceof InventoryScreen) return;
        if (boomSwitch.get() && mode.get() != Prot.Blast_Protection && explosionNear()) {
            mode.set(Prot.Blast_Protection);
            delayLeft = 0;
            didSkip = true;
        }
        if (delayLeft > 0) {
            delayLeft --;
            return;
        } else {
            delayLeft = delay.get();
        }
        Prot preMode = mode.get();
        if (didSkip) {
            delayLeft = switchCooldown.get();
            didSkip = false;
        }
        ItemStack itemStack;
        for (int a = 0; a < 4; a++) {
            itemStack = mc.player.inventory.getArmorStack(a);
            currentBest = 0;
            currentProt = 0;
            currentBlast = 0;
            currentFire = 0;
            currentProj = 0;
            currentArmour = 0;
            currentToughness = 0;
            currentUnbreaking = 0;
            currentMending = 0;
            if ((ignoreElytra.get() || ModuleManager.INSTANCE.get(ChestSwap.class).isActive()) && itemStack.getItem() == Items.ELYTRA) continue;
            if (EnchantmentHelper.hasBindingCurse(itemStack)) continue;
            if (itemStack.getItem() instanceof ArmorItem) {
                if (a == 1 && bProtLegs.get()) {
                    mode.set(Prot.Blast_Protection);
                }
                getCurrentScore(itemStack);
            }
            int bestSlot = -1;
            int bestScore = 0;
            for (int i = 0; i < 36; i++) {
                ItemStack stack = mc.player.inventory.getStack(i);
                if (stack.getItem() instanceof ArmorItem
                        && (((ArmorItem) stack.getItem()).getSlotType().getEntitySlotId() == a)) {
                    int temp = getItemScore(stack);
                    if (bestScore < temp) {
                        bestScore = temp;
                        bestSlot = i;
                    }
                }
            }
            if (bestSlot > -1) {
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(bestSlot), 0, SlotActionType.PICKUP);
                InvUtils.clickSlot(8 - a, 0, SlotActionType.PICKUP);
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(bestSlot), 0, SlotActionType.PICKUP);
                if (pause.get()) break;
            }
            mode.set(preMode);
        }
    });

    private int getItemScore(ItemStack itemStack){
        int score = 0;
        if (antiBreak.get() && (itemStack.getMaxDamage() - itemStack.getDamage()) <= breakDurability.get()) return 0;
        if (banBinding.get() && EnchantmentHelper.hasBindingCurse(itemStack)) return -10;
        Iterator<Enchantment> bad = avoidEnch.get().iterator();
        for (Enchantment ench = bad.next(); bad.hasNext(); ench = bad.next()) {
            if (EnchantmentHelper.getLevel(ench, itemStack) > 0) return 1;
        }
        score += 4 * (EnchantmentHelper.getLevel(mode.get().enchantment, itemStack) - currentBest);
        score += 2 * (EnchantmentHelper.getLevel(Enchantments.PROTECTION, itemStack) - currentProt);
        score += 2 * (EnchantmentHelper.getLevel(Enchantments.BLAST_PROTECTION, itemStack) - currentBlast);
        score += 2 * (EnchantmentHelper.getLevel(Enchantments.FIRE_PROTECTION, itemStack) - currentFire);
        score += 2 * (EnchantmentHelper.getLevel(Enchantments.PROJECTILE_PROTECTION, itemStack) - currentProj);
        score += 2 * (((ArmorItem) itemStack.getItem()).getProtection() - currentArmour);
        score += 2 * (((ArmorItem) itemStack.getItem()).method_26353() - currentToughness);
        score += EnchantmentHelper.getLevel(Enchantments.UNBREAKING, itemStack) - currentUnbreaking;
        if (preferMending.get() && (EnchantmentHelper.getLevel(Enchantments.MENDING, itemStack) - currentMending) > 0) score += weight.get();
        return score;
    }

    private void getCurrentScore(ItemStack itemStack) {
        currentBest = EnchantmentHelper.getLevel(mode.get().enchantment, itemStack);
        currentProt = EnchantmentHelper.getLevel(Enchantments.PROTECTION, itemStack);
        currentBlast = EnchantmentHelper.getLevel(Enchantments.BLAST_PROTECTION, itemStack);
        currentFire = EnchantmentHelper.getLevel(Enchantments.FIRE_PROTECTION, itemStack);
        currentProj = EnchantmentHelper.getLevel(Enchantments.PROJECTILE_PROTECTION, itemStack);
        currentArmour = ((ArmorItem) itemStack.getItem()).getProtection();
        currentToughness = ((ArmorItem) itemStack.getItem()).method_26353();
        currentUnbreaking = EnchantmentHelper.getLevel(Enchantments.UNBREAKING, itemStack);
        currentMending = EnchantmentHelper.getLevel(Enchantments.MENDING, itemStack);
    }

    private boolean explosionNear() {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity && DamageCalcUtils.crystalDamage(mc.player, entity.getPos()) > boomDamage.get()) {
                return true;
            }
        }
        if (!mc.world.getDimension().isBedWorking()) {
            for (BlockEntity blockEntity : mc.world.blockEntities) {
                BlockPos pos = blockEntity.getPos();
                if (blockEntity instanceof BedBlockEntity && DamageCalcUtils.bedDamage(mc.player, new Vec3d(pos.getX(), pos.getY(), pos.getZ())) > boomDamage.get()) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<Enchantment> setDefaultValue() {
        List<Enchantment> enchs = new ArrayList<>();
        enchs.add(Enchantments.BINDING_CURSE);
        enchs.add(Enchantments.FROST_WALKER);
        return enchs;
    }
}
