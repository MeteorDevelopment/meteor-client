/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ColorListSetting extends Setting<List<SettingColor>> {
    public ColorListSetting(String name, String description, List<SettingColor> defaultValue, Consumer<List<SettingColor>> onChanged, Consumer<Setting<List<SettingColor>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    protected List<SettingColor> parseImpl(String str) {
        // TODO: I cba to write a text parser for this
        return new ArrayList<>();
    }

    @Override
    protected boolean isValueValid(List<SettingColor> value) {
        return true;
    }

    @Override
    protected void resetImpl() {
        value = new ArrayList<>(defaultValue.size());

        for (SettingColor settingColor : defaultValue) {
            value.add(new SettingColor(settingColor));
        }
    }

    @Override
    protected NbtCompound save(NbtCompound tag) {
        tag.put("value", NbtUtils.listToTag(get()));

        return tag;
    }

    @Override
    protected List<SettingColor> load(NbtCompound tag) {
        get().clear();

        for (NbtElement e : tag.getList("value", NbtElement.COMPOUND_TYPE)) {
            get().add(new SettingColor().fromTag((NbtCompound) e));
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, List<SettingColor>, ColorListSetting> {
        public Builder() {
            super(new ArrayList<>());
        }

        @Override
        public ColorListSetting build() {
            return new ColorListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
