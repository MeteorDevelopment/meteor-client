/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class StatusEffectListSetting extends Setting<List<MobEffect>> {
    public StatusEffectListSetting(String name, String description, List<MobEffect> defaultValue, Consumer<List<MobEffect>> onChanged, Consumer<Setting<List<MobEffect>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    public void resetImpl() {
        value = new ArrayList<>(defaultValue);
    }

    @Override
    protected List<MobEffect> parseImpl(String str) {
        String[] values = str.split(",");
        List<MobEffect> effects = new ArrayList<>(values.length);

        try {
            for (String value : values) {
                MobEffect effect = parseId(BuiltInRegistries.STATUS_EFFECT, value);
                if (effect != null) effects.add(effect);
            }
        } catch (Exception ignored) {
        }

        return effects;
    }

    @Override
    protected boolean isValueValid(List<MobEffect> value) {
        return true;
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return BuiltInRegistries.STATUS_EFFECT.getIds();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag valueTag = new NbtList();

        for (MobEffect effect : get()) {
            Identifier id = BuiltInRegistries.STATUS_EFFECT.getId(effect);
            if (id != null) valueTag.add(StringTag.of(id.toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public List<MobEffect> load(CompoundTag tag) {
        get().clear();

        for (Tag tagI : tag.getListOrEmpty("value")) {
            MobEffect effect = BuiltInRegistries.STATUS_EFFECT.get(Identifier.of(tagI.asString().orElse("")));
            if (effect != null) get().add(effect);
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, List<MobEffect>, StatusEffectListSetting> {
        public Builder() {
            super(new ArrayList<>(0));
        }

        public Builder defaultValue(MobEffect... defaults) {
            return defaultValue(defaults != null ? Arrays.asList(defaults) : new ArrayList<>());
        }

        @Override
        public StatusEffectListSetting build() {
            return new StatusEffectListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
