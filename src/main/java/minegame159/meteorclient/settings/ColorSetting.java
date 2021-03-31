/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.settings;

import com.google.common.collect.ImmutableList;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.nbt.CompoundTag;

import java.util.List;
import java.util.function.Consumer;

public class ColorSetting extends Setting<SettingColor> {
    private static final List<String> SUGGESTIONS = ImmutableList.of("0 0 0 255", "225 25 25 255", "25 225 25 255", "25 25 225 255", "255 255 255 255");

    public ColorSetting(String name, String description, SettingColor defaultValue, Consumer<SettingColor> onChanged, Consumer<Setting<SettingColor>> onModuleActivated) {
        super(name, description, defaultValue, onChanged, onModuleActivated);
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
    public void reset(boolean callbacks) {
        if (value == null) value = new SettingColor(defaultValue);
        else value.set(defaultValue);

        if (callbacks) changed();
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
    public CompoundTag toTag() {
        CompoundTag tag = saveGeneral();
        tag.put("value", get().toTag());
        return tag;
    }

    @Override
    public SettingColor fromTag(CompoundTag tag) {
        get().fromTag(tag.getCompound("value"));

        changed();
        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private SettingColor defaultValue;
        private Consumer<SettingColor> onChanged;
        private Consumer<Setting<SettingColor>> onModuleActivated;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultValue(SettingColor defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder onChanged(Consumer<SettingColor> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder onModuleActivated(Consumer<Setting<SettingColor>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public ColorSetting build() {
            return new ColorSetting(name, description, defaultValue, onChanged, onModuleActivated);
        }
    }
}
