/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud;

import meteordevelopment.meteorclient.utils.Utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class HudElementInfo<T extends HudElement> {
    public final HudGroup group;
    public final String name;
    public final String title;
    public final String description;

    public final Supplier<T> factory;
    public final List<Preset> presets;

    public HudElementInfo(HudGroup group, String name, String title, String description, Supplier<T> factory) {
        this.group = group;
        this.name = name;
        this.title = title;
        this.description = description;

        this.factory = factory;
        this.presets = new ArrayList<>();
    }

    public HudElementInfo(HudGroup group, String name, String description, Supplier<T> factory) {
        this(group, name, Utils.nameToTitle(name), description, factory);
    }

    public Preset addPreset(String title, Consumer<T> callback) {
        Preset preset = new Preset(this, title, callback);

        presets.add(preset);
        presets.sort(Comparator.comparing(p -> p.title));

        return preset;
    }

    public boolean hasPresets() {
        return presets.size() > 0;
    }

    public HudElement create() {
        return factory.get();
    }

    public class Preset {
        public final HudElementInfo<?> info;
        public final String title;
        public final Consumer<T> callback;

        public Preset(HudElementInfo<?> info, String title, Consumer<T> callback) {
            this.info = info;
            this.title = title;
            this.callback = callback;
        }
    }
}
