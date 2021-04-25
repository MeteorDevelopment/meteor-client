/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.settings;

import minegame159.meteorclient.utils.misc.ISerializable;
import minegame159.meteorclient.utils.misc.NbtUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

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

    public <T> Setting<T> add(Setting<T> setting) {
        settings.add(setting);
        return setting;
    }

    @Override
    public Iterator<Setting<?>> iterator() {
        return settings.iterator();
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.putString("name", name);
        tag.putBoolean("sectionExpanded", sectionExpanded);
        tag.put("settings", NbtUtils.listToTag(settings));

        return tag;
    }

    @Override
    public SettingGroup fromTag(CompoundTag tag) {
        sectionExpanded = tag.getBoolean("sectionExpanded");

        ListTag settingsTag = tag.getList("settings", 10);
        for (Tag t : settingsTag) {
            CompoundTag settingTag = (CompoundTag) t;

            Setting<?> setting = get(settingTag.getString("name"));
            if (setting != null) setting.fromTag(settingTag);
        }

        return this;
    }
}
