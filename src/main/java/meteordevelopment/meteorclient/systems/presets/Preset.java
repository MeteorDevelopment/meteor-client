/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.presets;


import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

public class Preset<V, S extends Setting<V>> implements ISerializable<Preset<V, S>> {
    public String name;
    public S setting;

    public Preset(String name, S setting) {
        this.name = name;
        this.setting = setting;
    }

    public Preset(NbtElement tag) {
        fromTag((NbtCompound) tag);
    }

    public V get() {
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
    public Preset<V, S> fromTag(NbtCompound tag) {
        name = tag.getString("name");
        setting = (S) Setting.fromValueNBT((NbtCompound) tag.get("setting"));
        return this;
    }
}
