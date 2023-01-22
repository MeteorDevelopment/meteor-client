/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.presets;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

import java.util.List;

public class Preset<T extends Setting<?>> implements ISerializable<Preset<T>> {
    private List<NamedSetting> settings;

    public Preset() {}
    public Preset(NbtElement tag) {
        fromTag((NbtCompound) tag);
    }


    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.put("data", NbtUtils.listToTag(settings));
        return tag;
    }

    @Override
    public Preset<T> fromTag(NbtCompound tag) {
        settings = NbtUtils.listFromTag(tag.getList("data", 10), NamedSetting::new);
        return this;
    }

    private final class NamedSetting implements ISerializable<NamedSetting> {
        public String name;
        public T setting;

        public NamedSetting() {}
        public NamedSetting(NbtElement tag) {
            fromTag((NbtCompound) tag);
        }

        @Override
        public NbtCompound toTag() {
            NbtCompound tag = new NbtCompound();
            tag.putString("name", name);
            tag.put("setting", setting.toTag());
            return tag;
        }

        @Override
        public NamedSetting fromTag(NbtCompound tag) {
            name = tag.getString("name");
            setting.fromTag(tag.getCompound("setting"));
            return this;
        }
    }
}
