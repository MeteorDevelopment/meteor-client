/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.settings;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class StatusEffectListSetting extends Setting<List<StatusEffect>> {
    public StatusEffectListSetting(String name, String description, List<StatusEffect> defaultValue, Consumer<List<StatusEffect>> onChanged, Consumer<Setting<List<StatusEffect>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        value = new ArrayList<>(defaultValue);
    }

    @Override
    public void reset(boolean callbacks) {
        value = new ArrayList<>(defaultValue);
        if (callbacks) changed();
    }

    @Override
    protected List<StatusEffect> parseImpl(String str) {
        String[] values = str.split(",");
        List<StatusEffect> effects = new ArrayList<>(values.length);

        try {
            for (String value : values) {
                StatusEffect effect = parseId(Registry.STATUS_EFFECT, value);
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
        return Registry.STATUS_EFFECT.getIds();
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = saveGeneral();

        NbtList valueTag = new NbtList();

        for (StatusEffect effect : get()) {
            valueTag.add(NbtString.of(Registry.STATUS_EFFECT.getId(effect).toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public List<StatusEffect> fromTag(NbtCompound tag) {
        get().clear();

        NbtList valueTag = tag.getList("value", 8);
        for (NbtElement tagI : valueTag) {
            get().add(Registry.STATUS_EFFECT.get(new Identifier(tagI.asString())));
        }

        changed();
        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private List<StatusEffect> defaultValue;
        private Consumer<List<StatusEffect>> onChanged;
        private Consumer<Setting<List<StatusEffect>>> onModuleActivated;
        private IVisible visible;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultValue(List<StatusEffect> defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder onChanged(Consumer<List<StatusEffect>> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder onModuleActivated(Consumer<Setting<List<StatusEffect>>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public Builder visible(IVisible visible) {
            this.visible = visible;
            return this;
        }

        public StatusEffectListSetting build() {
            return new StatusEffectListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
