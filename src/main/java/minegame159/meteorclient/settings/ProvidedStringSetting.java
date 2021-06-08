/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.settings;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ProvidedStringSetting extends StringSetting {
    public final Supplier<String[]> supplier;

    public ProvidedStringSetting(String name, String description, String defaultValue, Consumer<String> onChanged, Consumer<Setting<String>> onModuleActivated, IVisible visible, Supplier<String[]> supplier) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        this.supplier = supplier;
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private String defaultValue;
        private Consumer<String> onChanged;
        private Consumer<Setting<String>> onModuleActivated;
        private IVisible visible;
        private Supplier<String[]> supplier;

        public minegame159.meteorclient.settings.ProvidedStringSetting.Builder name(String name) {
            this.name = name;
            return this;
        }

        public minegame159.meteorclient.settings.ProvidedStringSetting.Builder description(String description) {
            this.description = description;
            return this;
        }

        public minegame159.meteorclient.settings.ProvidedStringSetting.Builder defaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public minegame159.meteorclient.settings.ProvidedStringSetting.Builder onChanged(Consumer<String> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public minegame159.meteorclient.settings.ProvidedStringSetting.Builder onModuleActivated(Consumer<Setting<String>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public minegame159.meteorclient.settings.ProvidedStringSetting.Builder visible(IVisible visible) {
            this.visible = visible;
            return this;
        }

        public minegame159.meteorclient.settings.ProvidedStringSetting.Builder supplier(Supplier<String[]> supplier) {
            this.supplier = supplier;
            return this;
        }

        public ProvidedStringSetting build() {
            return new ProvidedStringSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, supplier);
        }
    }
}
