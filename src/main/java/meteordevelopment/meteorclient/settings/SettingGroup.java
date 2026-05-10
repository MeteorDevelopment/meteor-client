/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SettingGroup implements ISerializable<SettingGroup>, Iterable<Setting<?>> {
    public final String name;
    public boolean sectionExpanded;

    final List<Setting<?>> settings = new ArrayList<>(1);

    SettingGroup(String name, boolean sectionExpanded) {
        this.name = name;
        this.sectionExpanded = sectionExpanded;
    }

    public Setting<?> get(String name) {
        for (Setting<?> setting : this) {
            if (setting.name.equals(name)) return setting;
        }

        return null;
    }

    public <T extends Setting<?>> T add(T setting) {
        settings.add(setting);

        return setting;
    }

    public Setting<?> getByIndex(int index) {
        return settings.get(index);
    }

    public boolean wasChanged() {
        for (Setting<?> setting : settings) {
            if (setting.wasChanged()) return true;
        }
        return false;
    }

    @Override
    public @NotNull Iterator<Setting<?>> iterator() {
        return settings.iterator();
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.putString("name", name);
        tag.putBoolean("sectionExpanded", sectionExpanded);

        ListTag settingsTag = new ListTag();
        for (Setting<?> setting : this) {
            if (setting.wasChanged()) settingsTag.add(setting.toTag());
        }
        if (!settingsTag.isEmpty()) tag.put("settings", settingsTag);

        return tag;
    }

    @Override
    public SettingGroup fromTag(CompoundTag tag) {
        sectionExpanded = tag.getBooleanOr("sectionExpanded", false);

        ListTag settingsTag = tag.getListOrEmpty("settings");
        for (Tag t : settingsTag) {
            CompoundTag settingTag = (CompoundTag) t;

            Setting<?> setting = get(settingTag.getStringOr("name", ""));
            if (setting != null) setting.fromTag(settingTag);
        }

        return this;
    }
}
