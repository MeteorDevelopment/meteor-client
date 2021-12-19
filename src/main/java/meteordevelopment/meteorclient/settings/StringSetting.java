/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import net.minecraft.nbt.NbtCompound;

import java.util.function.Consumer;

public class StringSetting extends Setting<String> {
    public StringSetting(String name, String description, String defaultValue, Consumer<String> onChanged, Consumer<Setting<String>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    protected String parseImpl(String str) {
        return str;
    }

    @Override
    protected boolean isValueValid(String value) {
        return true;
    }

    @Override
    public NbtCompound save(NbtCompound tag) {
        tag.putString("value", get());

        return tag;
    }

    @Override
    public String load(NbtCompound tag) {
        set(tag.getString("value"));

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, String, StringSetting> {
        public Builder() {
            super(null);
        }

        @Override
        public StringSetting build() {
            return new StringSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
