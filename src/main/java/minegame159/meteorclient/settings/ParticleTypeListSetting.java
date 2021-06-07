/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.settings;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.particle.ParticleType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ParticleTypeListSetting extends Setting<List<ParticleType<?>>> {
    public ParticleTypeListSetting(String name, String description, List<ParticleType<?>> defaultValue, Consumer<List<ParticleType<?>>> onChanged, Consumer<Setting<List<ParticleType<?>>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        value = new ArrayList<>(defaultValue);
    }

    @Override
    public void reset(boolean callbacks) {
        value = new ArrayList<>(defaultValue);
        if (callbacks) changed();
    }

    @Override
    protected List<ParticleType<?>> parseImpl(String str) {
        String[] values = str.split(",");
        List<ParticleType<?>> particleTypes = new ArrayList<>(values.length);

        try {
            for (String value : values) {
                ParticleType<?> particleType = parseId(Registry.PARTICLE_TYPE, value);
                if (particleType != null) particleTypes.add(particleType);
            }
        } catch (Exception ignored) {}

        return particleTypes;
    }

    @Override
    protected boolean isValueValid(List<ParticleType<?>> value) {
        return true;
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Registry.PARTICLE_TYPE.getIds();
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = saveGeneral();

        ListTag valueTag = new ListTag();
        for (ParticleType<?> particleType : get()) {
            valueTag.add(StringTag.of(Registry.PARTICLE_TYPE.getId(particleType).toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public List<ParticleType<?>> fromTag(CompoundTag tag) {
        get().clear();

        ListTag valueTag = tag.getList("value", 8);
        for (Tag tagI : valueTag) {
            get().add(Registry.PARTICLE_TYPE.get(new Identifier(tagI.asString())));
        }

        changed();
        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private List<ParticleType<?>> defaultValue;
        private Consumer<List<ParticleType<?>>> onChanged;
        private Consumer<Setting<List<ParticleType<?>>>> onModuleActivated;
        private IVisible visible;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultValue(List<ParticleType<?>> defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder onChanged(Consumer<List<ParticleType<?>>> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder onModuleActivated(Consumer<Setting<List<ParticleType<?>>>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public Builder visible(IVisible visible) {
            this.visible = visible;
            return this;
        }

        public ParticleTypeListSetting build() {
            return new ParticleTypeListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
