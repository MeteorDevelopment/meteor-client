/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import com.google.common.base.Predicates;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class ParticleTypeListSetting extends AbstractRegistryListSetting<List<ParticleType<?>>, ParticleType<?>> {
    public ParticleTypeListSetting(String name, String description, List<ParticleType<?>> defaultValue, Consumer<List<ParticleType<?>>> onChanged, Consumer<Setting<List<ParticleType<?>>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible, Predicates.alwaysTrue(), Registries.PARTICLE_TYPE);
    }

    @Override
    protected List<ParticleType<?>> transferCollection(Collection<ParticleType<?>> from) {
        return new ArrayList<>(from);
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
