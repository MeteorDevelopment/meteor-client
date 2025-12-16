/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import net.minecraft.entity.Entity;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.tag.ItemTags;

public class AttributeSwap extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSmart = settings.createGroup("Smart Options");
    private final SettingGroup sgEnchants = settings.createGroup("Enchant Options");
    private final SettingGroup sgWeapon = settings.createGroup("Weapon Options");

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The mode to use.")
        .defaultValue(Mode.Simple)
        .build()
    );

    private final Setting<Integer> targetSlot = sgGeneral.add(new IntSetting.Builder()
        .name("target-slot")
        .description("Hotbar slot to swap to (1-9).")
        .defaultValue(1)
        .min(1)
        .sliderRange(1, 9)
        .visible(() -> mode.get() == Mode.Simple)
        .build()
    );

    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Swap back to the original slot after a delay.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> swapBackDelay = sgGeneral.add(new IntSetting.Builder()
        .name("swap-back-delay")
        .description("Delay in ticks before swapping back.")
        .defaultValue(2)
        .min(0)
        .sliderRange(0, 20)
        .visible(swapBack::get)
        .build()
    );

    private final Setting<Boolean> smartShieldBreak = sgSmart.add(new BoolSetting.Builder()
        .name("shield-breaker")
        .description("Automatically swaps to an axe if the target is blocking.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Smart)
        .build()
    );

    private final Setting<Boolean> smartEnchantSwap = sgSmart.add(new BoolSetting.Builder()
        .name("enchant-swap")
        .description("Swaps to an item with useful combat enchantments.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Smart)
        .build()
    );

    private final Setting<Boolean> smartDurability = sgSmart.add(new BoolSetting.Builder()
        .name("durability-saver")
        .description("Swaps to a non-damageable item to save durability on the main weapon.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Smart)
        .build()
    );

    private final Setting<Boolean> enchantFireAspect = sgEnchants.add(new BoolSetting.Builder()
        .name("fire-aspect")
        .description("Swaps to an item with Fire Aspect.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Smart && smartEnchantSwap.get())
        .build()
    );

    private final Setting<Boolean> enchantLooting = sgEnchants.add(new BoolSetting.Builder()
        .name("looting")
        .description("Swaps to an item with Looting.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Smart && smartEnchantSwap.get())
        .build()
    );

    private final Setting<Boolean> enchantBreach = sgEnchants.add(new BoolSetting.Builder()
        .name("breach")
        .description("Swaps to an item with Breach (Mace).")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Smart && smartEnchantSwap.get())
        .build()
    );

    private final Setting<Boolean> enchantDensity = sgEnchants.add(new BoolSetting.Builder()
        .name("density")
        .description("Swaps to an item with Density (Mace).")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Smart && smartEnchantSwap.get())
        .build()
    );

    private final Setting<Boolean> onlyOnWeapon = sgWeapon.add(new BoolSetting.Builder()
        .name("only-on-weapon")
        .description("Only swaps when holding a selected weapon in hand.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> sword = sgWeapon.add(new BoolSetting.Builder()
        .name("sword")
        .description("Works while holding a sword.")
        .defaultValue(true)
        .visible(onlyOnWeapon::get)
        .build()
    );

    private final Setting<Boolean> axe = sgWeapon.add(new BoolSetting.Builder()
        .name("axe")
        .description("Works while holding an axe.")
        .defaultValue(true)
        .visible(onlyOnWeapon::get)
        .build()
    );

    private final Setting<Boolean> pickaxe = sgWeapon.add(new BoolSetting.Builder()
        .name("pickaxe")
        .description("Works while holding a pickaxe.")
        .defaultValue(true)
        .visible(onlyOnWeapon::get)
        .build()
    );

    private final Setting<Boolean> shovel = sgWeapon.add(new BoolSetting.Builder()
        .name("shovel")
        .description("Works while holding a shovel.")
        .defaultValue(true)
        .visible(onlyOnWeapon::get)
        .build()
    );

    private final Setting<Boolean> hoe = sgWeapon.add(new BoolSetting.Builder()
        .name("hoe")
        .description("Works while holding a hoe.")
        .defaultValue(true)
        .visible(onlyOnWeapon::get)
        .build()
    );

    private final Setting<Boolean> mace = sgWeapon.add(new BoolSetting.Builder()
        .name("mace")
        .description("Works while holding a mace.")
        .defaultValue(true)
        .visible(onlyOnWeapon::get)
        .build()
    );

    private final Setting<Boolean> trident = sgWeapon.add(new BoolSetting.Builder()
        .name("trident")
        .description("Works while holding a trident.")
        .defaultValue(true)
        .visible(onlyOnWeapon::get)
        .build()
    );

    private int backTimer;
    private boolean awaitingBack;

    public AttributeSwap() {
        super(Categories.Combat, "attribute-swap", "Swaps to a target slot when you attack.");
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

        if (target != null && smartShieldBreak.get() && target instanceof LivingEntity living && living.isBlocking() && !(currentStack.getItem() instanceof AxeItem)) {
            int axeSlot = InvUtils.findInHotbar(item -> item.getItem() instanceof AxeItem).slot();
            if (axeSlot != -1) return axeSlot;
        }

        boolean isFalling = mc.player.fallDistance > 1.5;
        boolean durability = smartDurability.get();
        boolean enchantSwap = smartEnchantSwap.get();

        int bestSlot = -1;
        double bestScore = getItemScore(currentStack, target, isFalling, durability, enchantSwap);

        for (int i = 0; i < 9; i++) {
            if (i == mc.player.getInventory().getSelectedSlot()) continue;

            double score = getItemScore(mc.player.getInventory().getStack(i), target, isFalling, durability, enchantSwap);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    private double getItemScore(ItemStack stack, Entity target, boolean isFalling, boolean durability, boolean enchantSwap) {
        double score = 0;

        if (durability) score += getDurabilityScore(stack);
        if (enchantSwap && !stack.isEmpty()) score += getEnchantScore(stack, target, isFalling);

        return score;
    }

    private double getDurabilityScore(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageable()) return 4;

        int unbreaking = Utils.getEnchantmentLevel(stack, Enchantments.UNBREAKING);
        if (unbreaking > 0) return unbreaking * 0.05;

        return 0;
    }

    private double getEnchantScore(ItemStack stack, Entity target, boolean isFalling) {
        double score = 0;

        score += getFireAspectScore(stack, target);
        score += getLootingScore(stack, target);
        score += getBreachScore(stack, target);
        score += getDensityScore(stack, isFalling);

        return score;
    }

    private double getFireAspectScore(ItemStack stack, Entity target) {
        if (!enchantFireAspect.get() || target == null) return 0;
        int level = Utils.getEnchantmentLevel(stack, Enchantments.FIRE_ASPECT);
        return (level > 0 && !target.isOnFire()) ? 30 : 0;
    }

    private double getLootingScore(ItemStack stack, Entity target) {
        if (!enchantLooting.get() || target == null) return 0;
        int level = Utils.getEnchantmentLevel(stack, Enchantments.LOOTING);
        if (level > 0 && !(target instanceof PlayerEntity)) {
            boolean execute = (target instanceof LivingEntity l && l.getHealth() < 20) || target.isOnFire();
            return level * (execute ? 10 : 5);
        }
        return 0;
    }

    private double getBreachScore(ItemStack stack, Entity target) {
        if (!enchantBreach.get() || target == null) return 0;
        int level = Utils.getEnchantmentLevel(stack, Enchantments.BREACH);
        if (level > 0 && target instanceof LivingEntity l) {
            double armor = l.getAttributeValue(EntityAttributes.ARMOR);
            if (armor > 0) return level * armor * 0.3;
        }
        return 0;
    }

    private double getDensityScore(ItemStack stack, boolean isFalling) {
        if (!enchantDensity.get() || !isFalling) return 0;
        int level = Utils.getEnchantmentLevel(stack, Enchantments.DENSITY);
        if (level > 0) return 50 + (level * mc.player.fallDistance * 2);
        return 0;
    }

    public enum Mode {
        Simple,
        Smart
    }
}
