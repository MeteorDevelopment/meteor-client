/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.entity.effects;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.ints.IntObjectImmutablePair;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.objects.*;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.mixin.LivingEntityAccessor;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.meteorclient.utils.entity.StatusEffectHelper;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.Registries;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * @implNote status effect amplifiers, which are usually zero-indexed, are one-indexed here for ease of maths :thumbs_up:
 * @author Crosby
 */
public class StatusEffectBruteForce {
    private static final TrackedData<Integer> POTION_SWIRLS_COLOR = LivingEntityAccessor.meteor$getPotionSwirlsColor();
    private static final TrackedData<Boolean> POTION_SWRISL_AMBIENT = LivingEntityAccessor.meteor$getPotionSwirlsAmbient();
    private static final int EMPTY_COLOR = 3694022;
    private static final int MAX_DEPTH = 4;
    public static final Set<StatusEffectEntry> ALL_ENTRIES = new ReferenceOpenHashSet<>();
    public static final Set<StatusEffectEntry> BEACON_ENTRIES = new ReferenceOpenHashSet<>();
    private static final Map<LivingEntity, EntityEffectCache> PLAYER_EFFECT_MAP = new Object2ObjectOpenHashMap<>();
    private static final Object2ObjectMap<IntObjectPair<MutableParticleColor>, Map<StatusEffect, StatusEffectInstance>> EFFECT_CACHE_MAP = new Object2ObjectOpenHashMap<>();
    private static final Set<IntObjectPair<MutableParticleColor>> NULL_COLORS = new ObjectOpenHashSet<>();

    // status effects

    private static final StatusEffectEntry ABSORPTION = StatusEffectEntry.of(StatusEffects.ABSORPTION, 1);
    private static final StatusEffectEntry ABSORPTION_STRONG = StatusEffectEntry.of(StatusEffects.ABSORPTION, 4);

    @PreInit
    public static void initEntries() {
        MeteorClient.EVENT_BUS.subscribe(StatusEffectHelper.class);

        BEACON_ENTRIES.add(StatusEffectEntry.of(StatusEffects.STRENGTH, 1));
        BEACON_ENTRIES.add(StatusEffectEntry.of(StatusEffects.STRENGTH, 2));
        BEACON_ENTRIES.add(StatusEffectEntry.of(StatusEffects.JUMP_BOOST, 1));
        BEACON_ENTRIES.add(StatusEffectEntry.of(StatusEffects.JUMP_BOOST, 2));
        BEACON_ENTRIES.add(StatusEffectEntry.of(StatusEffects.REGENERATION, 1));
        BEACON_ENTRIES.add(StatusEffectEntry.of(StatusEffects.REGENERATION, 2));
        BEACON_ENTRIES.add(StatusEffectEntry.of(StatusEffects.RESISTANCE, 1));
        BEACON_ENTRIES.add(StatusEffectEntry.of(StatusEffects.RESISTANCE, 2));

        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.SPEED, 1));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.SPEED, 2));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.SLOWNESS, 1));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.SLOWNESS, 4));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.SLOWNESS, 6));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.HASTE, 1));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.HASTE, 2));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.MINING_FATIGUE, 1));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.STRENGTH, 1));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.STRENGTH, 2));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.JUMP_BOOST, 1));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.JUMP_BOOST, 2));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.NAUSEA, 1));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.REGENERATION, 1));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.REGENERATION, 2));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.RESISTANCE, 1));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.RESISTANCE, 2));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.FIRE_RESISTANCE, 1));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.WATER_BREATHING, 1));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.INVISIBILITY, 1));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.BLINDNESS, 1));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.NIGHT_VISION, 1));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.HUNGER, 1));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.WEAKNESS, 1));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.POISON, 1));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.POISON, 2));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.WITHER, 1));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.HEALTH_BOOST, 1));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.ABSORPTION, 1));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.ABSORPTION, 4));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.GLOWING, 1));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.LEVITATION, 1));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.LUCK, 1));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.UNLUCK, 1));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.SLOW_FALLING, 1));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.CONDUIT_POWER, 1));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.DOLPHINS_GRACE, 1));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.BAD_OMEN, 1));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.HERO_OF_THE_VILLAGE, 1));
        ALL_ENTRIES.add(StatusEffectEntry.of(StatusEffects.DARKNESS, 1));

        for (var statusEffectEntry : Registries.STATUS_EFFECT.getEntrySet()) {
            if (statusEffectEntry.getValue().isInstant()) continue;
            IntObjectPair<MutableParticleColor> cacheKey = new IntObjectImmutablePair<>(statusEffectEntry.getValue().getColor(), MutableParticleColor.EMPTY);
            EFFECT_CACHE_MAP.put(cacheKey, Map.of(statusEffectEntry.getValue(), new StatusEffectInstance(statusEffectEntry.getValue())));

            // Primitive modded compat
            if (!statusEffectEntry.getKey().getValue().getNamespace().equals("minecraft")) {
                ALL_ENTRIES.add(StatusEffectEntry.of(statusEffectEntry.getValue(), 1));
            }
        }
    }

    /**
     * Creates {@link EntityEffectCache} if missing, updates it if required.
     */
    public static EntityEffectCache fetch(LivingEntity entity) {
        int particleColor = entity.getDataTracker().get(POTION_SWIRLS_COLOR);
        if (isEmpty(particleColor)) return null;
        EntityEffectCache container = PLAYER_EFFECT_MAP.computeIfAbsent(entity, o -> new EntityEffectCache());
        if (particleColor != container.particleColor) update(particleColor, entity, container);
        return container;
    }

    @SuppressWarnings("UnstableApiUsage")
    private static void update(int particleColor, LivingEntity entity, EntityEffectCache container) {
        container.statusEffects.clear();
        container.particleColor = particleColor;

        MutableParticleColor initialColor = new MutableParticleColor();
        Set<StatusEffectEntry> possibleEntries;
        EffectAttributeModifier[] possibleModifiers;

        if (entity.getDataTracker().get(POTION_SWRISL_AMBIENT)) { // entity is only affected by effects from beacons
            possibleEntries = BEACON_ENTRIES;
            possibleModifiers = EffectAttributeModifier.BEACON;
        } else {
            // find status effects based on entity flags
            if (entity.isGlowing()) {
                initialColor.add(StatusEffects.GLOWING);
                container.add(StatusEffects.GLOWING);
            }
            if (entity.isInvisible()) {
                initialColor.add(StatusEffects.INVISIBILITY);
                container.add(StatusEffects.INVISIBILITY);
            }

            // find status effects based on tracked data
            int absorptionLevel = Math.round(entity.getAbsorptionAmount() / 4f);
            if (absorptionLevel <= 4) {
                possibleEntries = new ReferenceOpenHashSet<>(ALL_ENTRIES);
                possibleEntries.add(ABSORPTION_STRONG);
                if (absorptionLevel <= 1) possibleEntries.add(ABSORPTION);
            } else {
                possibleEntries = ALL_ENTRIES;
            }

            possibleModifiers = EffectAttributeModifier.ALL;
        }

        // find status effects based on tracked attributes
        AttributeContainer attributes = entity.getAttributes();
        for (var modifier : possibleModifiers) {
            if (attributes.hasModifierForAttribute(modifier.attribute(), modifier.id())) {
                double value = attributes.getModifierValue(modifier.attribute(), modifier.id());
                int amplifier = (int) Math.round(value / modifier.value());
                initialColor.add(modifier.effect(), amplifier);
                container.add(modifier.effect(), amplifier);
            }
        }

        // In order to minimize collisions, we hash both the particle color, and the initial state (attributes, tracked data, etc.) via the initial color
        IntObjectPair<MutableParticleColor> cacheKey = new IntObjectImmutablePair<>(particleColor, initialColor);

        // Map#computeIfAbsent(Object, Function) cannot cache null return values, so we use a separate cache for those
        if (NULL_COLORS.contains(cacheKey)) return;

        @Nullable Map<StatusEffect, StatusEffectInstance> match = EFFECT_CACHE_MAP.computeIfAbsent(cacheKey, key -> {
            for (int depth = 2; depth <= MAX_DEPTH; depth++) {
                for (var combination : Sets.combinations(possibleEntries, depth)) {
                    int color = blend(initialColor, combination);
                    if (color == particleColor) {
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
        else NULL_COLORS.add(cacheKey);
    }

    @EventHandler
    private static void onLeave(GameLeftEvent event) {
        PLAYER_EFFECT_MAP.clear();
    }

    private static boolean isEmpty(int particleColor) {
        return particleColor == 0 || particleColor == EMPTY_COLOR;
    }

    private static int blend(MutableParticleColor color, Iterable<StatusEffectEntry> entries) {
        float r = color.r;
        float g = color.g;
        float b = color.b;
        int a = color.a;

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

    public record StatusEffectEntry(StatusEffect effect, int amplifier, float r, float g, float b) {
        public static StatusEffectEntry of(StatusEffect effect, int amplifier) {
            int color = effect.getColor();
            float r = (float)(amplifier * (color >> 16 & 255)) / 255.0F;
            float g = (float)(amplifier * (color >> 8 & 255)) / 255.0F;
            float b = (float)(amplifier * (color & 255)) / 255.0F;
            return new StatusEffectEntry(effect, amplifier, r, g, b);
        }
    }
}
