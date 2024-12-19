/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.presets;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import net.minecraft.nbt.NbtCompound;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Presets extends System<Presets> implements Iterable<Preset<?>> {
    List<Preset<?>> presets = new ArrayList<>();

    public Presets() {
        super("presets");
    }

    public static Presets get() {
        return Systems.get(Presets.class);
    }

    public void add(Preset<?> preset) {
        presets.add(preset);
        save();
    }

    public <T> List<Preset<T>> getPresetForSetting(Setting<T> setting) {
        load();
        return presets.stream().filter(preset -> preset.setting.getClass() == setting.getClass()).map(preset -> (Preset<T>) preset).toList();
    }

    public void remove(Preset<?> preset) {
        if (presets.remove(preset)) {
            save();
        }
    }

    @Override
    public Iterator<Preset<?>> iterator() {
        return presets.iterator();
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.put("presets", NbtUtils.listToTag(presets));
        return tag;
    }

    @Override
    public Presets fromTag(NbtCompound tag) {
        presets = NbtUtils.listFromTag(tag.getList("presets", 10), Preset::new);
        return this;
    }
}
