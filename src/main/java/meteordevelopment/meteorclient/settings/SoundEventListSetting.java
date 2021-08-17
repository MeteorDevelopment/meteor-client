/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SoundEventListSetting extends Setting<List<SoundEvent>> {
    public SoundEventListSetting(String name, String description, List<SoundEvent> defaultValue, Consumer<List<SoundEvent>> onChanged, Consumer<Setting<List<SoundEvent>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        value = new ArrayList<>(defaultValue);
    }

    @Override
    public void reset(boolean callbacks) {
        value = new ArrayList<>(defaultValue);
        if (callbacks) changed();
    }

    @Override
    protected List<SoundEvent> parseImpl(String str) {
        String[] values = str.split(",");
        List<SoundEvent> sounds = new ArrayList<>(values.length);

        try {
            for (String value : values) {
                SoundEvent sound = parseId(Registry.SOUND_EVENT, value);
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
        return Registry.SOUND_EVENT.getIds();
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = saveGeneral();

        NbtList valueTag = new NbtList();
        for (SoundEvent sound : get()) {
            Identifier id = Registry.SOUND_EVENT.getId(sound);
            if (id != null) valueTag.add(NbtString.of(id.toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public List<SoundEvent> fromTag(NbtCompound tag) {
        get().clear();

        NbtList valueTag = tag.getList("value", 8);
        for (NbtElement tagI : valueTag) {
            SoundEvent soundEvent = Registry.SOUND_EVENT.get(new Identifier(tagI.asString()));
            if (soundEvent != null) get().add(soundEvent);
        }

        changed();
        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private List<SoundEvent> defaultValue;
        private Consumer<List<SoundEvent>> onChanged;
        private Consumer<Setting<List<SoundEvent>>> onModuleActivated;
        private IVisible visible;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultValue(List<SoundEvent> defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder onChanged(Consumer<List<SoundEvent>> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder onModuleActivated(Consumer<Setting<List<SoundEvent>>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public Builder visible(IVisible visible) {
            this.visible = visible;
            return this;
        }

        public SoundEventListSetting build() {
            return new SoundEventListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
