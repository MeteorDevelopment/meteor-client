/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.entity;

import com.google.common.collect.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.*;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.mixin.LivingEntityAccessor;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.Registries;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * @implNote status effect amplifiers, which are usually zero-indexed, are one-indexed here for ease of maths :thumbs_up:
 * @author Crosby
 */
public class StatusEffectManager {
    private static final TrackedData<Integer> POTION_SWIRLS_COLOR = LivingEntityAccessor.meteor$getPotionSwirlsColor();
    private static final int EMPTY_COLOR = 3694022;
    private static final int MAX_DEPTH = 3;
    private static final Set<StatusEffectEntry> ENTRIES = new ReferenceOpenHashSet<>();
    private static final Map<LivingEntity, EntityEffectCache> PLAYER_EFFECT_MAP = new Object2ObjectOpenHashMap<>();
    private static final Int2ObjectOpenHashMap<Map<StatusEffect, StatusEffectInstance>> EFFECT_CACHE_MAP = new Int2ObjectOpenHashMap<>();
    private static final IntSet NULL_COLORS = new IntOpenHashSet();

    @PreInit
    public static void initEntries() {
        MeteorClient.EVENT_BUS.subscribe(StatusEffectManager.class);

        ENTRIES.add(new StatusEffectEntry(StatusEffects.SPEED, 1));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.SPEED, 2));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.SLOWNESS, 1));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.SLOWNESS, 4));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.SLOWNESS, 6));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.HASTE, 1));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.HASTE, 2));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.MINING_FATIGUE, 1));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.STRENGTH, 1));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.STRENGTH, 2));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.JUMP_BOOST, 1));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.JUMP_BOOST, 2));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.NAUSEA, 1));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.REGENERATION, 1));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.REGENERATION, 2));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.RESISTANCE, 1));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.RESISTANCE, 2));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.FIRE_RESISTANCE, 1));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.WATER_BREATHING, 1));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.INVISIBILITY, 1));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.BLINDNESS, 1));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.NIGHT_VISION, 1));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.HUNGER, 1));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.WEAKNESS, 1));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.POISON, 1));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.POISON, 2));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.WITHER, 1));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.HEALTH_BOOST, 1));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.ABSORPTION, 1));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.ABSORPTION, 4));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.GLOWING, 1));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.LEVITATION, 1));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.LUCK, 1));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.UNLUCK, 1));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.SLOW_FALLING, 1));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.CONDUIT_POWER, 1));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.DOLPHINS_GRACE, 1));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.BAD_OMEN, 1));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.HERO_OF_THE_VILLAGE, 1));
        ENTRIES.add(new StatusEffectEntry(StatusEffects.DARKNESS, 1));

        for (var statusEffectEntry : Registries.STATUS_EFFECT.getEntrySet()) {
            if (statusEffectEntry.getValue().isInstant()) continue;
            EFFECT_CACHE_MAP.put(statusEffectEntry.getValue().getColor(), Map.of(statusEffectEntry.getValue(), new StatusEffectInstance(statusEffectEntry.getValue())));

            // Modded compat
            if (!statusEffectEntry.getKey().getValue().getNamespace().equals("minecraft")) {
                ENTRIES.add(new StatusEffectEntry(statusEffectEntry.getValue(), 1));
            }
        }
    }

    @Nullable
    public static StatusEffectInstance getStatusEffect(LivingEntity entity, StatusEffect effect) {
        if (entity == mc.player) return entity.getStatusEffect(effect);
        EntityEffectCache container = fetch(entity);
        return container == null ? null : container.statusEffects.get(effect);
    }

    public static boolean hasStatusEffect(LivingEntity entity, StatusEffect effect) {
        if (entity == mc.player) return entity.hasStatusEffect(effect);
        EntityEffectCache container = fetch(entity);
        return container != null && container.statusEffects.containsKey(effect);
    }

    public static Collection<StatusEffectInstance> getStatusEffects(LivingEntity entity) {
        if (entity == mc.player) return entity.getStatusEffects();
        EntityEffectCache container = fetch(entity);
        return container == null ? List.of() : container.statusEffects.values();
    }

    public static Map<StatusEffect, StatusEffectInstance> getActiveStatusEffects(LivingEntity entity) {
        if (entity == mc.player) return entity.getActiveStatusEffects();
        EntityEffectCache container = fetch(entity);
        return container == null ? Map.of() : container.statusEffects;
    }

    /**
     * Creates {@link EntityEffectCache} if missing, updates it if required.
     */
    private static EntityEffectCache fetch(LivingEntity entity) {
        int particleColor = entity.getDataTracker().get(POTION_SWIRLS_COLOR);
        if (isEmpty(particleColor)) return null;
        EntityEffectCache container = PLAYER_EFFECT_MAP.computeIfAbsent(entity, o -> new EntityEffectCache());
        if (particleColor != container.particleColor) update(particleColor, container);
        return container;
    }

    @SuppressWarnings("UnstableApiUsage")
    private static void update(int particleColor, EntityEffectCache container) {
        container.statusEffects.clear();
        container.particleColor = particleColor;

        // Map#computeIfAbsent(Object, Function) cannot cache null return values, so we use a separate cache for those
        if (NULL_COLORS.contains(particleColor)) return;

        @Nullable Map<StatusEffect, StatusEffectInstance> match = EFFECT_CACHE_MAP.computeIfAbsent(particleColor, _color -> {
            for (int depth = 2; depth <= MAX_DEPTH; depth++) {
                for (var combination : Sets.combinations(ENTRIES, depth)) {
                    int color = blend(combination);
                    if (color == _color) {
                        // If the amplifiers of all applied effects match, then it cannot be inferred and should be assumed to be 1
                        boolean assumeLowestAmplifier = combination.stream().mapToInt(o -> o.amplifier).reduce((i1, i2) -> i1 == i2 ? i1 : -1).orElse(-1) != -1;

                        Map<StatusEffect, StatusEffectInstance> map = new Reference2ObjectOpenHashMap<>();

                        for (var entry : combination) {
                            map.put(entry.effect, new StatusEffectInstance(entry.effect, 0, assumeLowestAmplifier ? 0 : entry.amplifier - 1));
                        }

                        return map;
                    }
                }
            }
            return null;
        });
        if (match != null) container.statusEffects.putAll(match);
        else NULL_COLORS.add(particleColor);
    }

    @EventHandler
    private static void onLeave(GameLeftEvent event) {
        PLAYER_EFFECT_MAP.clear();
    }

    private static boolean isEmpty(int particleColor) {
        return particleColor == 0 || particleColor == EMPTY_COLOR;
    }

    private static int blend(Iterable<StatusEffectEntry> entries) {
        float r = 0f;
        float g = 0f;
        float b = 0f;
        int a = 0;

        for (var entry : entries) {
            r += entry.r;
            g += entry.g;
            b += entry.b;
            a += entry.amplifier;
        }

        r = r / (float) a * 255.0F;
        g = g / (float) a * 255.0F;
        b = b / (float) a * 255.0F;

        return (int) r << 16 | (int) g << 8 | (int) b;
    }

    private static class EntityEffectCache {
        private final Map<StatusEffect, StatusEffectInstance> statusEffects = new Reference2ReferenceOpenHashMap<>();
        private int particleColor;
    }

    private static class StatusEffectEntry {
        private final StatusEffect effect;
        private final int amplifier;
        private final float r;
        private final float g;
        private final float b;

        private StatusEffectEntry(StatusEffect effect, int amplifier) {
            this.effect = effect;
            this.amplifier = amplifier;

            int color = effect.getColor();
            r = (float)(amplifier * (color >> 16 & 255)) / 255.0F;
            g = (float)(amplifier * (color >> 8 & 255)) / 255.0F;
            b = (float)(amplifier * (color & 255)) / 255.0F;
        }
    }
}
