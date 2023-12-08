/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import com.google.common.collect.ImmutableList;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.nbt.NbtCompound;

import java.util.List;
import java.util.function.Consumer;

public class ColorSetting extends Setting<SettingColor> {
    private static final List<String> SUGGESTIONS = ImmutableList.of("0 0 0 255", "225 25 25 255", "25 225 25 255", "25 25 225 255", "255 255 255 255");

    public ColorSetting(String name, String description, SettingColor defaultValue, Consumer<SettingColor> onChanged, Consumer<Setting<SettingColor>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    protected SettingColor parseImpl(String str) {
        try {
            String[] strs = str.split(" ");
            return new SettingColor(Integer.parseInt(strs[0]), Integer.parseInt(strs[1]), Integer.parseInt(strs[2]), Integer.parseInt(strs[3]));
        } catch (IndexOutOfBoundsException | NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    public void resetImpl() {
        if (value == null) value = new SettingColor(defaultValue);
        else value.set(defaultValue);
    }

    @Override
    protected boolean isValueValid(SettingColor value) {
        value.validate();

        return true;
    }

    @Override
    public List<String> getSuggestions() {
        return SUGGESTIONS;
    }

    @Override
    protected NbtCompound save(NbtCompound tag) {
        tag.put("value", get().toTag());

        return tag;
    }

    @Override
    public SettingColor load(NbtCompound tag) {
        get().fromTag(tag.getCompound("value"));

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, SettingColor, ColorSetting> {
        public Builder() {
            super(new SettingColor());
        }

        @Override
        public ColorSetting build() {
            return new ColorSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }

        @Override
        public Builder defaultValue(SettingColor defaultValue) {
            this.defaultValue.set(defaultValue);
            return this;
        }

        public Builder defaultValue(Color defaultValue) {
            this.defaultValue.set(defaultValue);
            return this;
        }
    }
}
