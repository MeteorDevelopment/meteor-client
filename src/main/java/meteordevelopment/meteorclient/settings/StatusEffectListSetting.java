/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class StatusEffectListSetting extends Setting<List<StatusEffect>> {
    public StatusEffectListSetting(String name, String description, List<StatusEffect> defaultValue, Consumer<List<StatusEffect>> onChanged, Consumer<Setting<List<StatusEffect>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    public void resetImpl() {
        value = new ArrayList<>(defaultValue);
    }

    @Override
    protected List<StatusEffect> parseImpl(String str) {
        String[] values = str.split(",");
        List<StatusEffect> effects = new ArrayList<>(values.length);

        try {
            for (String value : values) {
                StatusEffect effect = parseId(Registries.STATUS_EFFECT, value);
                if (effect != null) effects.add(effect);
            }
        } catch (Exception ignored) {}

        return effects;
    }

    @Override
    protected boolean isValueValid(List<StatusEffect> value) {
        return true;
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Registries.STATUS_EFFECT.getIds();
    }

    @Override
    public NbtCompound save(NbtCompound tag) {
        NbtList valueTag = new NbtList();

        for (StatusEffect effect : get()) {
            Identifier id = Registries.STATUS_EFFECT.getId(effect);
            if (id != null) valueTag.add(NbtString.of(id.toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public List<StatusEffect> load(NbtCompound tag) {
        get().clear();

        NbtList valueTag = tag.getList("value", 8);
        for (NbtElement tagI : valueTag) {
            StatusEffect effect = Registries.STATUS_EFFECT.get(Identifier.of(tagI.asString()));
            if (effect != null) get().add(effect);
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, List<StatusEffect>, StatusEffectListSetting> {
        public Builder() {
            super(new ArrayList<>(0));
        }

        public Builder defaultValue(StatusEffect... defaults) {
            return defaultValue(defaults != null ? Arrays.asList(defaults) : new ArrayList<>());
        }

        @Override
        public StatusEffectListSetting build() {
            return new StatusEffectListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
