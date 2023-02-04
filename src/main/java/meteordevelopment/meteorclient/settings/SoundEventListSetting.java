/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

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
                SoundEvent sound = parseId(Registries.SOUND_EVENT, value);
                if (sound != null) sounds.add(sound);
            }
        } catch (Exception ignored) {}

        return sounds;
    }

    @Override
    protected boolean isValueValid(List<SoundEvent> value) {
        return true;
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Registries.SOUND_EVENT.getIds();
    }

    @Override
    public NbtCompound save(NbtCompound tag) {
        NbtList valueTag = new NbtList();
        for (SoundEvent sound : get()) {
            Identifier id = Registries.SOUND_EVENT.getId(sound);
            if (id != null) valueTag.add(NbtString.of(id.toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public List<SoundEvent> load(NbtCompound tag) {
        get().clear();

        NbtList valueTag = tag.getList("value", 8);
        for (NbtElement tagI : valueTag) {
            SoundEvent soundEvent = Registries.SOUND_EVENT.get(new Identifier(tagI.asString()));
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
