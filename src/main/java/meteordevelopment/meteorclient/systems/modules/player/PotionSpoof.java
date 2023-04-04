/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.StatusEffectInstanceAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.List;

import static net.minecraft.entity.effect.StatusEffects.*;

public class PotionSpoof extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Object2IntMap<StatusEffect>> spoofPotions = sgGeneral.add(new StatusEffectAmplifierMapSetting.Builder()
        .name("spoofed-potions")
        .description("Potions to add.")
        .defaultValue(Utils.createStatusEffectMap())
        .build()
    );

    private final Setting<Boolean> clearEffects = sgGeneral.add(new BoolSetting.Builder()
        .name("clear-effects")
        .description("Clears effects on module disable.")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<StatusEffect>> antiPotion = sgGeneral.add(new StatusEffectListSetting.Builder()
        .name("blocked-potions")
        .description("Potions to block.")
        .defaultValue(
            LEVITATION,
            JUMP_BOOST,
            SLOW_FALLING,
            DOLPHINS_GRACE
        )
        .build()
    );

    public final Setting<Boolean> applyGravity = sgGeneral.add(new BoolSetting.Builder()
        .name("gravity")
        .description("Applies gravity when levitating.")
        .defaultValue(false)
        .build()
    );

    public PotionSpoof() {
        super(Categories.Player, "potion-spoof", "Spoofs potion statuses for you. SOME effects DO NOT work.");
    }

    @Override
    public void onDeactivate() {
        if (!clearEffects.get() || !Utils.canUpdate()) return;

        for (StatusEffect effect : spoofPotions.get().keySet()) {
            if (spoofPotions.get().getInt(effect) <= 0) continue;
            if (mc.player.hasStatusEffect(effect)) mc.player.removeStatusEffect(effect);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        for (StatusEffect statusEffect : spoofPotions.get().keySet()) {
            int level = spoofPotions.get().getInt(statusEffect);
            if (level <= 0) continue;

            if (mc.player.hasStatusEffect(statusEffect)) {
                StatusEffectInstance instance = mc.player.getStatusEffect(statusEffect);
                ((StatusEffectInstanceAccessor) instance).setAmplifier(level - 1);
                if (instance.getDuration() < 20) ((StatusEffectInstanceAccessor) instance).setDuration(20);
            } else {
                mc.player.addStatusEffect(new StatusEffectInstance(statusEffect, 20, level - 1));
            }
        }
    }

    public boolean shouldBlock(StatusEffect effect) {
        return isActive() && antiPotion.get().contains(effect);
    }
}
