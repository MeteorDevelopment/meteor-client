/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import com.google.common.base.Predicates;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class SoundEventListSetting extends AbstractRegistryListSetting<List<SoundEvent>, SoundEvent> {
    public SoundEventListSetting(String name, String description, List<SoundEvent> defaultValue, Consumer<List<SoundEvent>> onChanged, Consumer<Setting<List<SoundEvent>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible, Predicates.alwaysTrue(), Registries.SOUND_EVENT);
    }

    @Override
    protected List<SoundEvent> transferCollection(Collection<SoundEvent> from) {
        return new ArrayList<>(from);
    }

    public static class Builder extends SettingBuilder<Builder, List<SoundEvent>, SoundEventListSetting> {
        public Builder() {
            super(new ArrayList<>(0));
        }

        public Builder defaultValue(SoundEvent... defaults) {
            return defaultValue(defaults != null ? Arrays.asList(defaults) : new ArrayList<>());
        }

        @Override
        public SoundEventListSetting build() {
            return new SoundEventListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
