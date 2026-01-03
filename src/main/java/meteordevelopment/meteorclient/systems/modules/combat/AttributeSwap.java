/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.registry.tag.ItemTags;

public class AttributeSwap extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSwappingOptions = settings.createGroup("swapping-ptions");
    private final SettingGroup sgSwordEnchants = settings.createGroup("sword-enchants");
    private final SettingGroup sgMaceEnchants = settings.createGroup("mace-enchants");
    private final SettingGroup sgOtherEnchants = settings.createGroup("other-enchants");
    private final SettingGroup sgWeapon = settings.createGroup("weapon-options");

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .defaultValue(Mode.Simple)
        .build()
    );

    private final Setting<Integer> targetSlot = sgGeneral.add(new IntSetting.Builder()
        .name("target-slot")
        .defaultValue(1)
        .min(1)
        .sliderRange(1, 9)
        .visible(() -> mode.get() == Mode.Simple)
        .build()
    );

    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
        .name("swap-back")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> swapBackDelay = sgGeneral.add(new IntSetting.Builder()
        .name("swap-back-delay")
        .defaultValue(2)
        .min(0)
        .max(100)
        .sliderRange(0, 20)
        .visible(swapBack::get)
        .build()
    );

    private final Setting<Boolean> smartShieldBreak = sgSwappingOptions.add(new BoolSetting.Builder()
        .name("shield-breaker")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Smart)
        .build()
    );

    private final Setting<Boolean> smartDurability = sgSwappingOptions.add(new BoolSetting.Builder()
        .name("durability-saver")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Smart)
        .build()
    );

    private final Setting<Boolean> swordSwapping = sgSwappingOptions.add(new BoolSetting.Builder()
        .name("sword-swapping")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Smart)
        .build()
    );

    private final Setting<Boolean> maceSwapping = sgSwappingOptions.add(new BoolSetting.Builder()
        .name("mace-swapping")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Smart)
        .build()
    );

    private final Setting<Boolean> otherSwapping = sgSwappingOptions.add(new BoolSetting.Builder()
        .name("other-swapping")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Smart)
        .build()
    );

    private final Setting<Boolean> enchantFireAspect = sgSwordEnchants.add(new BoolSetting.Builder()
        .name("fire-aspect")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Smart && swordSwapping.get())
        .build()
    );

    private final Setting<Boolean> enchantLooting = sgSwordEnchants.add(new BoolSetting.Builder()
        .name("looting")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Smart && swordSwapping.get())
        .build()
    );

    private final Setting<Boolean> enchantSharpness = sgSwordEnchants.add(new BoolSetting.Builder()
        .name("sharpness")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Smart && swordSwapping.get())
        .build()
    );

    private final Setting<Boolean> enchantSmite = sgSwordEnchants.add(new BoolSetting.Builder()
        .name("smite")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Smart && swordSwapping.get())
        .build()
    );

    private final Setting<Boolean> enchantBaneOfArthropods = sgSwordEnchants.add(new BoolSetting.Builder()
        .name("bane-of-arthropods")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Smart && swordSwapping.get())
        .build()
    );

    private final Setting<Boolean> enchantSweepingEdge = sgSwordEnchants.add(new BoolSetting.Builder()
        .name("sweeping-edge")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Smart && swordSwapping.get())
        .build()
    );

    private final Setting<Boolean> regularMace = sgMaceEnchants.add(new BoolSetting.Builder()
        .name("regular-mace")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Smart && maceSwapping.get())
        .build()
    );

    private final Setting<Boolean> enchantDensity = sgMaceEnchants.add(new BoolSetting.Builder()
        .name("density")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Smart && maceSwapping.get())
        .build()
    );

    private final Setting<Boolean> enchantBreach = sgMaceEnchants.add(new BoolSetting.Builder()
        .name("breach")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Smart && maceSwapping.get())
        .build()
    );

    private final Setting<Boolean> enchantWindBurst = sgMaceEnchants.add(new BoolSetting.Builder()
        .name("wind-burst")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Smart && maceSwapping.get())
        .build()
    );

    private final Setting<Boolean> enchantImpaling = sgOtherEnchants.add(new BoolSetting.Builder()
        .name("impaling")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Smart && otherSwapping.get())
        .build()
    );

    private final Setting<Boolean> onlyOnWeapon = sgWeapon.add(new BoolSetting.Builder()
        .name("only-on-weapon")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> sword = sgWeapon.add(new BoolSetting.Builder()
        .name("sword")
        .defaultValue(true)
        .visible(onlyOnWeapon::get)
        .build()
    );

    private final Setting<Boolean> axe = sgWeapon.add(new BoolSetting.Builder()
        .name("axe")
        .defaultValue(true)
        .visible(onlyOnWeapon::get)
        .build()
    );

    private final Setting<Boolean> pickaxe = sgWeapon.add(new BoolSetting.Builder()
        .name("pickaxe")
        .defaultValue(true)
        .visible(onlyOnWeapon::get)
        .build()
    );

    private final Setting<Boolean> shovel = sgWeapon.add(new BoolSetting.Builder()
        .name("shovel")
        .defaultValue(true)
        .visible(onlyOnWeapon::get)
        .build()
    );

    private final Setting<Boolean> hoe = sgWeapon.add(new BoolSetting.Builder()
        .name("hoe")
        .defaultValue(true)
        .visible(onlyOnWeapon::get)
        .build()
    );

    private final Setting<Boolean> mace = sgWeapon.add(new BoolSetting.Builder()
        .name("mace")
        .defaultValue(true)
        .visible(onlyOnWeapon::get)
        .build()
    );

    private final Setting<Boolean> trident = sgWeapon.add(new BoolSetting.Builder()
        .name("trident")
        .defaultValue(true)
        .visible(onlyOnWeapon::get)
        .build()
    );

    private int backTimer;
    private boolean awaitingBack;

    public AttributeSwap() {
        super(Categories.Combat, "attribute-swap");
    }

    @Override
    public void onDeactivate() {
        backTimer = 0;
        awaitingBack = false;
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (!canSwapByWeapon()) return;
        performSwap(event.entity);
    }

    private void performSwap(Entity target) {
        if (awaitingBack) return;

        int slotIndex;

        if (mode.get() == Mode.Simple) {
            slotIndex = targetSlot.get() - 1;
        } else {
            slotIndex = getSmartSlot(target);
        }

        if (slotIndex < 0 || slotIndex > 8) return;
        if (slotIndex == mc.player.getInventory().getSelectedSlot()) return;

        if (!InvUtils.swap(slotIndex, swapBack.get())) return;

        awaitingBack = swapBack.get();
        if (awaitingBack) backTimer = swapBackDelay.get();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!awaitingBack) return;
        if (backTimer-- > 0) return;
        InvUtils.swapBack();
        awaitingBack = false;
    }

    private boolean canSwapByWeapon() {
        if (!onlyOnWeapon.get()) return true;
        return InvUtils.testInMainHand(item ->
            (sword.get() && item.isIn(ItemTags.SWORDS)) ||
                (axe.get() && item.isIn(ItemTags.AXES)) ||
                (pickaxe.get() && item.isIn(ItemTags.PICKAXES)) ||
                (shovel.get() && item.isIn(ItemTags.SHOVELS)) ||
                (hoe.get() && item.isIn(ItemTags.HOES)) ||
                (mace.get() && item.getItem() instanceof MaceItem) ||
                (trident.get() && item.getItem() instanceof TridentItem)
        );
    }

    private int getSmartSlot(Entity target) {
        ItemStack currentStack = mc.player.getMainHandStack();

        if (target != null && smartShieldBreak.get() && target instanceof LivingEntity living && living.isBlocking()) {
            if (currentStack.getItem() instanceof AxeItem) return -1;
            int axeSlot = InvUtils.findInHotbar(item -> item.getItem() instanceof AxeItem).slot();
            if (axeSlot != -1) return axeSlot;
        }

        boolean isFalling = mc.player.fallDistance > 1.5;
        boolean durability = smartDurability.get();

        boolean isLiving = target instanceof LivingEntity;
        boolean isPlayer = target instanceof PlayerEntity;
        boolean isOnFire = target != null && target.isOnFire();
        boolean isUndead = target != null && target.getType().isIn(EntityTypeTags.SENSITIVE_TO_SMITE);
        boolean isArthropod = target != null && target.getType().isIn(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS);
        boolean isAquatic = target != null && target.getType().isIn(EntityTypeTags.SENSITIVE_TO_IMPALING);
        boolean hasFireResistance = isLiving && (((LivingEntity) target).hasStatusEffect(StatusEffects.FIRE_RESISTANCE) || hasFireProtectionArmor((LivingEntity) target));
        double armor = isLiving ? ((LivingEntity) target).getAttributeValue(EntityAttributes.ARMOR) : 0;
        float health = isLiving ? ((LivingEntity) target).getHealth() : 0;

        int bestSlot = -1;
        double bestScore = getItemScore(currentStack, isFalling, durability, isLiving, isPlayer, isOnFire, hasFireResistance, isUndead, isArthropod, isAquatic, armor, health);

        for (int i = 0; i < 9; i++) {
            if (i == mc.player.getInventory().getSelectedSlot()) continue;

            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty() && !durability) continue;

            double score = getItemScore(stack, isFalling, durability, isLiving, isPlayer, isOnFire, hasFireResistance, isUndead, isArthropod, isAquatic, armor, health);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    private double getItemScore(ItemStack stack, boolean isFalling, boolean durability, boolean isLiving, boolean isPlayer, boolean isOnFire, boolean hasFireResistance, boolean isUndead, boolean isArthropod, boolean isAquatic, double armor, float health) {
        double score = 0;

        if (durability) {
            score += getDurabilityScore(stack);
        }

        if (stack.isEmpty()) return score;

        score += getCombatScore(stack, isFalling, isLiving, isPlayer, isOnFire, hasFireResistance, isUndead, isArthropod, isAquatic, armor, health);

        return score;
    }

    private double getDurabilityScore(ItemStack stack) {
        if (!stack.isDamageable()) return 4;

        int unbreaking = Utils.getEnchantmentLevel(stack, Enchantments.UNBREAKING);
        if (unbreaking > 0) return unbreaking * 0.05;

        return 0;
    }

    private double getCombatScore(ItemStack stack, boolean isFalling, boolean isLiving, boolean isPlayer, boolean isOnFire, boolean hasFireResistance, boolean isUndead, boolean isArthropod, boolean isAquatic, double armor, float health) {
        double score = 0;

        if (swordSwapping.get()) {
            score += getFireAspectScore(stack, isOnFire, hasFireResistance);
            score += getLootingScore(stack, isPlayer, isLiving, isOnFire, health);
            score += getSharpnessScore(stack, isOnFire);
            score += getSmiteScore(stack, isUndead, isOnFire);
            score += getBaneOfArthropodsScore(stack, isArthropod, isOnFire);
            score += getSweepingEdgeScore(stack);
        }
        if (maceSwapping.get()) {
            score += getBreachScore(stack, isLiving, armor);
            score += getDensityScore(stack, isFalling);
            score += getWindBurstScore(stack, isFalling);
            score += getMaceScore(stack, isFalling);
        }
        if (otherSwapping.get()) {
            score += getImpalingScore(stack, isAquatic);
        }

        return score;
    }

    private double getFireAspectScore(ItemStack stack, boolean isOnFire, boolean hasFireResistance) {
        if (!enchantFireAspect.get() || isOnFire || hasFireResistance) return 0;
        int level = Utils.getEnchantmentLevel(stack, Enchantments.FIRE_ASPECT);
        return (level > 0) ? 30 : 0;
    }

    private double getLootingScore(ItemStack stack, boolean isPlayer, boolean isLiving, boolean isOnFire, float health) {
        if (!enchantLooting.get() || isPlayer) return 0;
        int level = Utils.getEnchantmentLevel(stack, Enchantments.LOOTING);
        if (level > 0) {
            boolean execute = (isLiving && health < 20) || isOnFire;
            return level * (execute ? 10 : 5);
        }
        return 0;
    }

    private double getSharpnessScore(ItemStack stack, boolean isOnFire) {
        if (!enchantSharpness.get()) return 0;
        int level = Utils.getEnchantmentLevel(stack, Enchantments.SHARPNESS);
        if (level > 0) {
            double baseScore = (1 + 0.5 * (level - 1)) * 3;
            return isOnFire ? baseScore * 1.5 : baseScore;
        }
        return 0;
    }

    private double getSmiteScore(ItemStack stack, boolean isUndead, boolean isOnFire) {
        if (!enchantSmite.get() || !isUndead) return 0;
        int level = Utils.getEnchantmentLevel(stack, Enchantments.SMITE);
        if (level > 0) {
            double baseScore = level * 5;
            return isOnFire ? baseScore * 1.5 : baseScore;
        }
        return 0;
    }

    private double getBaneOfArthropodsScore(ItemStack stack, boolean isArthropod, boolean isOnFire) {
        if (!enchantBaneOfArthropods.get() || !isArthropod) return 0;
        int level = Utils.getEnchantmentLevel(stack, Enchantments.BANE_OF_ARTHROPODS);
        if (level > 0) {
            double baseScore = level * 5;
            return isOnFire ? baseScore * 1.5 : baseScore;
        }
        return 0;
    }

    private double getSweepingEdgeScore(ItemStack stack) {
        if (!enchantSweepingEdge.get()) return 0;
        int level = Utils.getEnchantmentLevel(stack, Enchantments.SWEEPING_EDGE);
        if (level > 0) {
            return level * 3;
        }
        return 0;
    }

    private double getImpalingScore(ItemStack stack, boolean isAquatic) {
        if (!enchantImpaling.get() || !isAquatic) return 0;

        int level = Utils.getEnchantmentLevel(stack, Enchantments.IMPALING);
        if (level > 0) {
            return level * 5;
        }
        return 0;
    }

    private double getBreachScore(ItemStack stack, boolean isLiving, double armor) {
        if (!enchantBreach.get() || !isLiving || armor <= 0) return 0;
        int level = Utils.getEnchantmentLevel(stack, Enchantments.BREACH);
        if (level > 0) {
            return level * armor * 0.3;
        }
        return 0;
    }

    private double getDensityScore(ItemStack stack, boolean isFalling) {
        if (!enchantDensity.get() || !isFalling) return 0;
        int level = Utils.getEnchantmentLevel(stack, Enchantments.DENSITY);
        if (level > 0) return 50 + (level * mc.player.fallDistance * 2);
        return 0;
    }

    private double getWindBurstScore(ItemStack stack, boolean isFalling) {
        if (!enchantWindBurst.get() || !isFalling) return 0;
        int level = Utils.getEnchantmentLevel(stack, Enchantments.WIND_BURST);
        if (level > 0) return level * 20;
        return 0;
    }

    private double getMaceScore(ItemStack stack, boolean isFalling) {
        if (!regularMace.get() || !isFalling) return 0;
        if (stack.getItem() instanceof MaceItem) return 40;
        return 0;
    }

    private boolean hasFireProtectionArmor(LivingEntity entity) {
        for (EquipmentSlot slot : AttributeModifierSlot.ARMOR) {
            ItemStack stack = entity.getEquippedStack(slot);
            if (stack.isEmpty()) continue;

            int fireProtection = Utils.getEnchantmentLevel(stack, Enchantments.FIRE_PROTECTION);
            if (fireProtection > 0) return true;
        }
        return false;
    }

    public enum Mode {
        Simple,
        Smart
    }
}
