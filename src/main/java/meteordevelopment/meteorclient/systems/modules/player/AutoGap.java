/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import baritone.api.BaritoneAPI;
import meteordevelopment.meteorclient.events.entity.player.ItemUseCrosshairTargetEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.AnchorAura;
import meteordevelopment.meteorclient.systems.modules.combat.BedAura;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AutoGap extends Module {
    private static final Class<? extends Module>[] AURAS = new Class[] { KillAura.class, CrystalAura.class, AnchorAura.class, BedAura.class };

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPotions = settings.createGroup("Potions");
    private final SettingGroup sgHealth = settings.createGroup("Health");

    // General

    private final Setting<Boolean> preferEGap = sgGeneral.add(new BoolSetting.Builder()
            .name("prefer-egap")
            .description("Prefers to eat E-Gap over Gaps if found.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> always = sgGeneral.add(new BoolSetting.Builder()
            .name("always")
            .description("If it should always eat.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> pauseAuras = sgGeneral.add(new BoolSetting.Builder()
            .name("pause-auras")
            .description("Pauses all auras when eating.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> pauseBaritone = sgGeneral.add(new BoolSetting.Builder()
            .name("pause-baritone")
            .description("Pause baritone when eating.")
            .defaultValue(true)
            .build()
    );

    // Potions

    private final Setting<Boolean> potionsRegeneration = sgPotions.add(new BoolSetting.Builder()
            .name("potions-regeneration")
            .description("If it should eat when Regeneration runs out.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> potionsFireResistance = sgPotions.add(new BoolSetting.Builder()
            .name("potions-fire-resistance")
            .description("If it should eat when Fire Resistance runs out. Requires E-Gaps.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> potionsResistance = sgPotions.add(new BoolSetting.Builder()
            .name("potions-absorption")
            .description("If it should eat when Resistance runs out. Requires E-Gaps.")
            .defaultValue(false)
            .build()
    );

    // Health

    private final Setting<Boolean> healthEnabled = sgHealth.add(new BoolSetting.Builder()
            .name("health-enabled")
            .description("If it should eat when health drops below threshold.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> healthThreshold = sgHealth.add(new IntSetting.Builder()
            .name("health-threshold")
            .description("Health threshold to eat at. Includes absorption.")
            .defaultValue(20)
            .min(0)
            .sliderMax(40)
            .build()
    );

    private boolean requiresEGap;

    private boolean eating;
    private int slot, prevSlot;

    private final List<Class<? extends Module>> wasAura = new ArrayList<>();
    private boolean wasBaritone;

    public AutoGap() {
        super(Categories.Player, "auto-gap", "Automatically eats Gaps or E-Gaps.");
    }

    @Override
    public void onDeactivate() {
        if (eating) stopEating();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (eating) {
            // If we are eating check if we should still be still eating
            if (shouldEat()) {
                // Check if the item in current slot is not gap or egap
                if (isNotGapOrEGap(mc.player.getInventory().getStack(slot))) {
                    // If not try finding a new slot
                    int slot = findSlot();

                    // If no valid slot was found then stop eating
                    if (slot == -1) {
                        stopEating();
                        return;
                    }
                    // Otherwise change to the new slot
                    else {
                        changeSlot(slot);
                    }
                }

                // Continue eating
                eat();
            }
            // If we shouldn't be eating anymore then stop
            else {
                stopEating();
            }
        }
        else {
            // If we are not eating check if we should start eating
            if (shouldEat()) {
                // Try to find a valid slot
                slot = findSlot();

                // If slot was found then start eating
                if (slot != -1) startEating();
            }
        }
    }

    @EventHandler
    private void onItemUseCrosshairTarget(ItemUseCrosshairTargetEvent event) {
        if (eating) event.target = null;
    }

    private void startEating() {
        prevSlot = mc.player.getInventory().selectedSlot;
        eat();

        // Pause auras
        wasAura.clear();
        if (pauseAuras.get()) {
            for (Class<? extends Module> klass : AURAS) {
                Module module = Modules.get().get(klass);

                if (module.isActive()) {
                    wasAura.add(klass);
                    module.toggle();
                }
            }
        }

        // Pause baritone
        wasBaritone = false;
        if (pauseBaritone.get() && BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
            wasBaritone = true;
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("pause");
        }
    }

    private void eat() {
        changeSlot(slot);
        setPressed(true);
        if (!mc.player.isUsingItem()) Utils.rightClick();

        eating = true;
    }

    private void stopEating() {
        changeSlot(prevSlot);
        setPressed(false);

        eating = false;

        // Resume auras
        if (pauseAuras.get()) {
            for (Class<? extends Module> klass : AURAS) {
                Module module = Modules.get().get(klass);

                if (wasAura.contains(klass) && !module.isActive()) {
                    module.toggle();
                }
            }
        }

        // Resume baritone
        if (pauseBaritone.get() && wasBaritone) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume");
        }
    }

    private void setPressed(boolean pressed) {
        mc.options.useKey.setPressed(pressed);
    }

    private void changeSlot(int slot) {
        InvUtils.swap(slot, false);
        this.slot = slot;
    }

    private boolean shouldEat() {
        requiresEGap = false;

        if (always.get()) return true;
        if (shouldEatPotions()) return true;
        return shouldEatHealth();
    }

    private boolean shouldEatPotions() {
        Map<StatusEffect, StatusEffectInstance> effects = mc.player.getActiveStatusEffects();

        // Regeneration
        if (potionsRegeneration.get() && !effects.containsKey(StatusEffects.REGENERATION)) return true;

        // Fire resistance
        if (potionsFireResistance.get() && !effects.containsKey(StatusEffects.FIRE_RESISTANCE)) {
            requiresEGap = true;
            return true;
        }

        // Absorption
        if (potionsResistance.get() && !effects.containsKey(StatusEffects.RESISTANCE)) {
            requiresEGap = true;
            return true;
        }

        return false;
    }

    private boolean shouldEatHealth() {
        if (!healthEnabled.get()) return false;

        int health = Math.round(mc.player.getHealth() + mc.player.getAbsorptionAmount());
        return health < healthThreshold.get();
    }

    private int findSlot() {
        boolean preferEGap = this.preferEGap.get();
        if (requiresEGap) preferEGap = true;

        int slot = -1;
        Item currentItem = null;

        for (int i = 0; i < 9; i++) {
            // Skip if item stack is empty
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            // Skip if item isn't a gap or egap
            if (isNotGapOrEGap(stack)) continue;
            Item item = stack.getItem();

            // If this is the first apple found then set it without looking at preferEGap setting
            if (currentItem == null) {
                slot = i;
                currentItem = item;
            }
            else {
                // Skip if current item and item are the same
                if (currentItem == item) continue;

                // If egap was found and preferEGap is true we can return the current slot
                if (item == Items.ENCHANTED_GOLDEN_APPLE && preferEGap) {
                    slot = i;
                    currentItem = item;

                    break;
                }
                // If gap was found and preferEGap is false we can return the current slot
                else if (item == Items.GOLDEN_APPLE && !preferEGap) {
                    slot = i;
                    currentItem = item;

                    break;
                }
            }
        }

        // If requiresEGap is true but no egap was found return -1
        if (requiresEGap && currentItem != Items.ENCHANTED_GOLDEN_APPLE) return -1;

        return slot;
    }

    private boolean isNotGapOrEGap(ItemStack stack) {
        Item item = stack.getItem();
        return item != Items.GOLDEN_APPLE && item != Items.ENCHANTED_GOLDEN_APPLE;
    }

    public boolean isEating() {
        return isActive() && eating;
    }
}
