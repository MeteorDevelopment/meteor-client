/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.presets;


import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

public class Preset<T extends Setting<?>> implements ISerializable<Preset<T>> {
    public String name;
    public T setting;

    public Preset(String name, T setting) {
        this.name = name;
        this.setting = setting;
    }

    public Preset(NbtElement tag) {
        fromTag((NbtCompound) tag);
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.putString("name", name);
        tag.put("setting", setting.toPresetTag());
        return tag;
    }

    @Override
    public Preset<T> fromTag(NbtCompound tag) {
        name = tag.getString("name");
        setting.fromTag(tag.getCompound("setting"));
        return this;
    }
}
