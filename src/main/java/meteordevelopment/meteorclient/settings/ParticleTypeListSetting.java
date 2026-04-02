/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ParticleTypeListSetting extends Setting<List<ParticleType<?>>> {
    public ParticleTypeListSetting(String name, String description, List<ParticleType<?>> defaultValue, Consumer<List<ParticleType<?>>> onChanged, Consumer<Setting<List<ParticleType<?>>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    public void resetImpl() {
        value = new ArrayList<>(defaultValue);
    }

    @Override
    protected List<ParticleType<?>> parseImpl(String str) {
        String[] values = str.split(",");
        List<ParticleType<?>> particleTypes = new ArrayList<>(values.length);

        try {
            for (String value : values) {
                ParticleType<?> particleType = parseId(BuiltInRegistries.PARTICLE_TYPE, value);
                if (particleType instanceof ParticleOptions) particleTypes.add(particleType);
            }
        } catch (Exception ignored) {
        }

        return particleTypes;
    }

    @Override
    protected boolean isValueValid(List<ParticleType<?>> value) {
        return true;
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return BuiltInRegistries.PARTICLE_TYPE.keySet();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag valueTag = new ListTag();
        for (ParticleType<?> particleType : get()) {
            Identifier id = BuiltInRegistries.PARTICLE_TYPE.getKey(particleType);
            if (id != null) valueTag.add(StringTag.valueOf(id.toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public List<ParticleType<?>> load(CompoundTag tag) {
        get().clear();

        ListTag valueTag = tag.getListOrEmpty("value");
        for (Tag tagI : valueTag) {
            ParticleType<?> particleType = BuiltInRegistries.PARTICLE_TYPE.getValue(Identifier.parse(tagI.asString().orElse("")));
            if (particleType != null) get().add(particleType);
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, List<ParticleType<?>>, ParticleTypeListSetting> {
        public Builder() {
            super(new ArrayList<>(0));
        }

        public Builder defaultValue(ParticleType<?>... defaults) {
            return defaultValue(defaults != null ? Arrays.asList(defaults) : new ArrayList<>());
        }

        @Override
        public ParticleTypeListSetting build() {
            return new ParticleTypeListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
