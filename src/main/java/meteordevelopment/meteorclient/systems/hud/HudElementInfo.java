/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud;

import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.translation.TranslationKey;
import meteordevelopment.meteorclient.translation.TranslationManager;
import meteordevelopment.meteorclient.utils.Utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class HudElementInfo<T extends HudElement> {
    public final HudGroup group;
    public final String name;

    private TranslationKey nameKey;
    private TranslationKey descriptionKey;

    public final Supplier<T> factory;
    public final List<Preset> presets;

    public HudElementInfo(HudGroup group, String name, String title, String description, Supplier<T> factory) {
        this.group = group;
        this.name = name;
        this.nameKey = TranslationKey.of(TranslationManager.hudNameKey(name));
        this.descriptionKey = TranslationKey.of(TranslationManager.hudDescriptionKey(name));

        this.factory = factory;
        this.presets = new ArrayList<>();
    }

    public HudElementInfo(HudGroup group, String name, String description, Supplier<T> factory) {
        this(group, name, Utils.nameToTitle(name), description, factory);
    }

    /**
     * Re-scope this element's translation keys to an addon namespace. Called by
     * {@link Hud#register(HudElementInfo)} for elements owned by an addon, because the
     * owning class is not known when the info object is constructed.
     */
    public void assignNamespace(String namespace) {
        if (namespace == null || namespace.equals(TranslationManager.METEOR_MOD_ID)) return;
        this.nameKey = TranslationKey.of(TranslationManager.hudNameKey(namespace, name));
        this.descriptionKey = TranslationKey.of(TranslationManager.hudDescriptionKey(namespace, name));
    }

    /** The HUD element title in the current language, resolved lazily. */
    public String title() {
        return nameKey.get();
    }

    /** The HUD element description in the current language, resolved lazily. */
    public String description() {
        return descriptionKey.get();
    }

    public TranslationKey nameKey() {
        return nameKey;
    }

    public TranslationKey descriptionKey() {
        return descriptionKey;
    }

    public Preset addPreset(String title, Consumer<T> callback) {
        Preset preset = new Preset(this, title, callback);

        presets.add(preset);
        presets.sort(Comparator.comparing(p -> p.title));

        return preset;
    }

    public boolean hasPresets() {
        return !presets.isEmpty();
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
