/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import it.unimi.dsi.fastutil.objects.Reference2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;

import java.util.function.Consumer;

public class StatusEffectAmplifierMapSetting extends Setting<Reference2IntMap<MobEffect>> {
    public static final Reference2IntMap<MobEffect> EMPTY_STATUS_EFFECT_MAP = createStatusEffectMap();

    public StatusEffectAmplifierMapSetting(String name, String description, Reference2IntMap<MobEffect> defaultValue, Consumer<Reference2IntMap<MobEffect>> onChanged, Consumer<Setting<Reference2IntMap<MobEffect>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    public void resetImpl() {
        value = new Reference2IntOpenHashMap<>(defaultValue);
    }

    @Override
    protected Reference2IntMap<MobEffect> parseImpl(String str) {
        String[] values = str.split(",");
        Reference2IntMap<MobEffect> effects = new Reference2IntOpenHashMap<>(EMPTY_STATUS_EFFECT_MAP);

        try {
            for (String value : values) {
                String[] split = value.split(" ");

                MobEffect effect = parseId(BuiltInRegistries.MOB_EFFECT, split[0]);
                int level = Integer.parseInt(split[1]);

                effects.put(effect, level);
            }
        } catch (Exception ignored) {
        }

        return effects;
    }

    @Override
    protected boolean isValueValid(Reference2IntMap<MobEffect> value) {
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag valueTag = new CompoundTag();
        for (MobEffect statusEffect : get().keySet()) {
            Identifier id = BuiltInRegistries.MOB_EFFECT.getKey(statusEffect);
            if (id != null) valueTag.putInt(id.toString(), get().getInt(statusEffect));
        }
        tag.put("value", valueTag);

        return tag;
    }

    private static Reference2IntMap<MobEffect> createStatusEffectMap() {
        Reference2IntMap<MobEffect> map = new Reference2IntArrayMap<>(BuiltInRegistries.MOB_EFFECT.keySet().size());

        BuiltInRegistries.MOB_EFFECT.forEach(potion -> map.put(potion, 0));

        return map;
    }

    @Override
    public Reference2IntMap<MobEffect> load(CompoundTag tag) {
        get().clear();

        CompoundTag valueTag = tag.getCompoundOrEmpty("value");
        for (String key : valueTag.keySet()) {
            MobEffect statusEffect = BuiltInRegistries.MOB_EFFECT.getValue(Identifier.parse(key));
            if (statusEffect != null) get().put(statusEffect, valueTag.getIntOr(key, 0));
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, Reference2IntMap<MobEffect>, StatusEffectAmplifierMapSetting> {
        public Builder() {
            super(new Reference2IntOpenHashMap<>(0));
        }

        @Override
        public StatusEffectAmplifierMapSetting build() {
            return new StatusEffectAmplifierMapSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
