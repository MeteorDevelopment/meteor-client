/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.settings;

import net.minecraft.nbt.CompoundTag;

import java.util.function.Consumer;

public class StringSetting extends Setting<String> {
    public StringSetting(String name, String description, String defaultValue, Consumer<String> onChanged, Consumer<Setting<String>> onModuleActivated) {
        super(name, description, defaultValue, onChanged, onModuleActivated);

        value = defaultValue;
    }

    @Override
    protected String parseImpl(String str) {
        return str;
    }

    @Override
    public void reset(boolean callbacks) {
        value = defaultValue;
        if (callbacks) changed();
    }

    @Override
    protected boolean isValueValid(String value) {
        return true;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = saveGeneral();
        tag.putString("value", get());
        return tag;
    }

    @Override
    public String fromTag(CompoundTag tag) {
        set(tag.getString("value"));

        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private String defaultValue;
        private Consumer<String> onChanged;
        private Consumer<Setting<String>> onModuleActivated;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder onChanged(Consumer<String> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder onModuleActivated(Consumer<Setting<String>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public StringSetting build() {
            return new StringSetting(name, description, defaultValue, onChanged, onModuleActivated);
        }
    }
}
