/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.settings;

import minegame159.meteorclient.gui.widgets.WTable;
import minegame159.meteorclient.utils.misc.ISerializable;
import minegame159.meteorclient.utils.misc.NbtUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Settings implements ISerializable<Settings>, Iterable<SettingGroup> {
    private SettingGroup defaultGroup;
    private final List<SettingGroup> groups = new ArrayList<>(1);

    public Setting<?> get(String name) {
        for (SettingGroup sg : this) {
            for (Setting<?> setting : sg) {
                if (name.equalsIgnoreCase(setting.name)) return setting;
            }
        }

        return null;
    }

    public SettingGroup getGroup(String name) {
        for (SettingGroup sg : this) {
            if (sg.name.equals(name)) return sg;
        }

        return null;
    }

    public int sizeGroups() {
        return groups.size();
    }

    public SettingGroup getDefaultGroup() {
        if (defaultGroup == null) defaultGroup = createGroup("General");
        return defaultGroup;
    }

    public SettingGroup createGroup(String name, boolean expanded) {
        SettingGroup group = new SettingGroup(name, expanded);
        groups.add(group);
        return group;
    }
    public SettingGroup createGroup(String name) {
        return createGroup(name, true);
    }

    public WTable createTable(boolean activate) {
        WTable table = new WTable();

        for (SettingGroup sg : this) {
            for (Setting<?> setting : sg) {
                if (activate) setting.onActivated();
                setting.resetWidget();
            }
        }

        for (SettingGroup group : groups) {
            group.fillTable(table);
        }

        return table;
    }
    public WTable createTable() {
        return createTable(true);
    }

    @Override
    public Iterator<SettingGroup> iterator() {
        return groups.iterator();
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.put("groups", NbtUtils.listToTag(groups));
        return tag;
    }

    @Override
    public Settings fromTag(CompoundTag tag) {
        ListTag groupsTag = tag.getList("groups", 10);

        for (Tag t : groupsTag) {
            CompoundTag groupTag = (CompoundTag) t;

            SettingGroup sg = getGroup(groupTag.getString("name"));
            if (sg != null) sg.fromTag(groupTag);
        }

        return this;
    }
}
