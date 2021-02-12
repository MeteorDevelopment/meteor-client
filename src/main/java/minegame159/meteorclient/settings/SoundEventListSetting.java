/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.settings;

import minegame159.meteorclient.gui.screens.settings.SoundEventListSettingScreen;
import minegame159.meteorclient.gui.widgets.WButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SoundEventListSetting extends Setting<List<SoundEvent>> {
    public SoundEventListSetting(String name, String description, List<SoundEvent> defaultValue, Consumer<List<SoundEvent>> onChanged, Consumer<Setting<List<SoundEvent>>> onModuleActivated) {
        super(name, description, defaultValue, onChanged, onModuleActivated);

        value = new ArrayList<>(defaultValue);

        widget = new WButton("Select");
        ((WButton) widget).action = () -> MinecraftClient.getInstance().openScreen(new SoundEventListSettingScreen(this));
    }

    @Override
    public void reset(boolean callbacks) {
        value = new ArrayList<>(defaultValue);
        if (callbacks) {
            resetWidget();
            changed();
        }
    }

    @Override
    protected List<SoundEvent> parseImpl(String str) {
        String[] values = str.split(",");
        List<SoundEvent> sounds = new ArrayList<>(1);

        for (String value : values) {
            String val = value.trim();
            Identifier id;
            if (val.contains(":")) id = new Identifier(val);
            else id = new Identifier("minecraft", val);
            sounds.add(Registry.SOUND_EVENT.get(id));
        }

        return sounds;
    }

    @Override
    public void resetWidget() {

    }

    @Override
    protected boolean isValueValid(List<SoundEvent> value) {
        return true;
    }

    @Override
    protected String generateUsage() {
        return "(highlight)sound id (default)(block_anvil_hit, minecraft:entity_bat_hurt, etc)";
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = saveGeneral();

        ListTag valueTag = new ListTag();
        for (SoundEvent sound : get()) {
            valueTag.add(StringTag.of(Registry.SOUND_EVENT.getId(sound).toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public List<SoundEvent> fromTag(CompoundTag tag) {
        get().clear();

        ListTag valueTag = tag.getList("value", 8);
        for (Tag tagI : valueTag) {
            get().add(Registry.SOUND_EVENT.get(new Identifier(tagI.asString())));
        }

        changed();
        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private List<SoundEvent> defaultValue;
        private Consumer<List<SoundEvent>> onChanged;
        private Consumer<Setting<List<SoundEvent>>> onModuleActivated;

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

        public SoundEventListSetting build() {
            return new SoundEventListSetting(name, description, defaultValue, onChanged, onModuleActivated);
        }
    }
}
