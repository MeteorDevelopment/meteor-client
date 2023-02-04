/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.presets;


import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

public class Preset<T> implements ISerializable<Preset<T>> {
    public String name;
    public Setting<T> setting;

    public Preset(String name, Setting<T> setting) {
        this.name = name;
        this.setting = setting;
    }

    public Preset(NbtElement tag) {
        fromTag((NbtCompound) tag);
    }

    public T get() {
        return setting.get();
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.putString("name", name);
        tag.put("setting", setting.getValueNBT());
        return tag;
    }

    @Override
    public Preset<T> fromTag(NbtCompound tag) {
        name = tag.getString("name");
        setting = Setting.fromValueNBT((NbtCompound) tag.get("setting"));
        return this;
    }
}
