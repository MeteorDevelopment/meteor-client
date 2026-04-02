/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class SoundEventListSetting extends Setting<List<SoundEvent>> {
    public SoundEventListSetting(String name, String description, List<SoundEvent> defaultValue, Consumer<List<SoundEvent>> onChanged, Consumer<Setting<List<SoundEvent>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    public void resetImpl() {
        value = new ArrayList<>(defaultValue);
    }

    @Override
    protected List<SoundEvent> parseImpl(String str) {
        String[] values = str.split(",");
        List<SoundEvent> sounds = new ArrayList<>(values.length);

        try {
            for (String value : values) {
                SoundEvent sound = parseId(BuiltInRegistries.SOUND_EVENT, value);
                if (sound != null) sounds.add(sound);
            }
        } catch (Exception ignored) {
        }

        return sounds;
    }

    @Override
    protected boolean isValueValid(List<SoundEvent> value) {
        return true;
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return BuiltInRegistries.SOUND_EVENT.keySet();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag valueTag = new ListTag();
        for (SoundEvent sound : get()) {
            Identifier id = BuiltInRegistries.SOUND_EVENT.getKey(sound);
            if (id != null) valueTag.add(StringTag.valueOf(id.toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public List<SoundEvent> load(CompoundTag tag) {
        get().clear();

        for (Tag tagI : tag.getListOrEmpty("value")) {
            SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.getValue(Identifier.parse(tagI.asString().orElse("")));
            if (soundEvent != null) get().add(soundEvent);
        }

        return get();
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
