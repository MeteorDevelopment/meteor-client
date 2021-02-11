/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

import baritone.api.BaritoneAPI;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.entity.player.ItemUseCrosshairTargetEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.mixininterface.IKeyBinding;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.combat.CrystalAura;
import minegame159.meteorclient.modules.combat.KillAura;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.player.InvUtils;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AutoEat extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgHunger = settings.createGroup("Hunger");

    // General

    private final Setting<Boolean> egaps = sgGeneral.add(new BoolSetting.Builder()
            .name("egaps")
            .description("Eats enchanted golden apples.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> gaps = sgGeneral.add(new BoolSetting.Builder()
            .name("gaps")
            .description("Eats golden apples.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> chorus = sgGeneral.add(new BoolSetting.Builder()
            .name("chorus")
            .description("Eats chorus fruit.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> noBad = sgGeneral.add(new BoolSetting.Builder()
            .name("filter-negative-effects")
            .description("Filters out food items that give you negative potion effects.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> disableAuras = sgGeneral.add(new BoolSetting.Builder()
            .name("disable-auras")
            .description("Disable all auras when using this module.")
            .defaultValue(false)
            .build()
    );

    // Hunger

    private final Setting<Boolean> autoHunger = sgHunger.add(new BoolSetting.Builder()
            .name("auto-eat")
            .description("Automatically eats whenever it can.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> minHunger = sgHunger.add(new IntSetting.Builder()
            .name("min-hunger")
            .description("The level of hunger you eat at.")
            .defaultValue(17)
            .min(1)
            .max(19)
            .sliderMax(19)
            .build()
    );

    private boolean wasKillActive = false;
    private boolean wasCrystalActive = false;
    private boolean isEating;
    private int preSelectedSlot, preFoodLevel;
    private int slot;
    private boolean wasThis = false;

    public AutoEat() {
        super(Category.Player, "auto-eat", "Automatically eats food.");
    }

    @Override
    public void onDeactivate() {
        if (isEating) {
            ((IKeyBinding) mc.options.keyUse).setPressed(false);
            isEating = false;
            if (preSelectedSlot != -1) mc.player.inventory.selectedSlot = preSelectedSlot;
            if (wasThis) BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume"); wasThis = false;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player.abilities.creativeMode) return;
        if (isEating && !mc.player.getMainHandStack().getItem().isFood()) ((IKeyBinding) mc.options.keyUse).setPressed(false);

        slot = -1;
        int bestHunger = -1;

        for (int i = 0; i < 9; i++) {
            Item item = mc.player.inventory.getStack(i).getItem();
            if (!item.isFood()) continue;
            if (noBad.get()) {
                if (item == Items.POISONOUS_POTATO || item == Items.PUFFERFISH || item == Items.CHICKEN
                        || item == Items.ROTTEN_FLESH || item == Items.SPIDER_EYE || item == Items.SUSPICIOUS_STEW) continue;
            }

            if (item == Items.ENCHANTED_GOLDEN_APPLE && item.getFoodComponent().getHunger() > bestHunger) {
                if (egaps.get()) {
                    bestHunger = item.getFoodComponent().getHunger();
                    slot = i;
                }
            } else if (item == Items.GOLDEN_APPLE && item.getFoodComponent().getHunger() > bestHunger) {
                if (gaps.get()) {
                    bestHunger = item.getFoodComponent().getHunger();
                    slot = i;
                }
            } else if (item == Items.CHORUS_FRUIT && item.getFoodComponent().getHunger() > bestHunger) {
                if (chorus.get()) {
                    bestHunger = item.getFoodComponent().getHunger();
                    slot = i;
                }
            } else if (item.getFoodComponent().getHunger() > bestHunger) {
                bestHunger = item.getFoodComponent().getHunger();
                slot = i;
            }
        }
        if(mc.player.getOffHandStack().isFood() && mc.player.getOffHandStack().getItem().getFoodComponent().getHunger() > bestHunger){
            bestHunger = mc.player.getOffHandStack().getItem().getFoodComponent().getHunger();
            slot = InvUtils.OFFHAND_SLOT;
        }

        if (isEating) {
            if (mc.player.getHungerManager().getFoodLevel() > preFoodLevel || slot == -1) {
                isEating = false;
                mc.interactionManager.stopUsingItem(mc.player);
                ((IKeyBinding) mc.options.keyUse).setPressed(false);
                if(wasKillActive){
                    Modules.get().get(KillAura.class).toggle();
                    wasKillActive = false;
                }
                if(wasCrystalActive){
                    Modules.get().get(CrystalAura.class).toggle();
                    wasCrystalActive = false;
                }
                if (preSelectedSlot != -1) mc.player.inventory.selectedSlot = preSelectedSlot;
                if (wasThis) BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume"); wasThis = false;

                return;
            }

            if(slot != InvUtils.OFFHAND_SLOT) {
                mc.player.inventory.selectedSlot = slot;
            }

            if (!mc.player.isUsingItem()) {
                if (disableAuras.get()) {
                    if (Modules.get().get(KillAura.class).isActive()) {
                        wasKillActive = true;
                        Modules.get().get(KillAura.class).toggle();
                    }
                    if (Modules.get().get(CrystalAura.class).isActive()) {
                        wasCrystalActive = true;
                    }
                }

                ((IKeyBinding) mc.options.keyUse).setPressed(true);
                if (slot == InvUtils.OFFHAND_SLOT) {
                    mc.interactionManager.interactItem(mc.player, mc.world, Hand.OFF_HAND);
                } else {
                    mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
                }
            }

            return;
        }

        if (slot != -1 && (20 - mc.player.getHungerManager().getFoodLevel() >= bestHunger && autoHunger.get()) || (20 - mc.player.getHungerManager().getFoodLevel() >= minHunger.get() && autoHunger.get())) {
            preSelectedSlot = mc.player.inventory.selectedSlot;
            if(slot != InvUtils.OFFHAND_SLOT && slot != -1) {
                mc.player.inventory.selectedSlot = slot;
            }
            isEating = true;
            preFoodLevel = mc.player.getHungerManager().getFoodLevel();

            if (BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
                BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("pause");
                wasThis = true;
            }
        }
    }

    @EventHandler
    private void onItemUseCrosshairTarget(ItemUseCrosshairTargetEvent event) {
        if (isActive() && isEating) event.target = null;
    }
}
