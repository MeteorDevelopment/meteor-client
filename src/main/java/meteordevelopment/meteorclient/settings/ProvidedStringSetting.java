/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ProvidedStringSetting extends StringSetting {
    public final Supplier<String[]> supplier;

    public ProvidedStringSetting(String name, String description, String defaultValue, Consumer<String> onChanged, Consumer<Setting<String>> onModuleActivated, IVisible visible, boolean wide, Class<? extends WTextBox.Renderer> renderer, Supplier<String[]> supplier) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible, wide, renderer);

        this.supplier = supplier;
    }

    public static class Builder extends SettingBuilder<Builder, String, ProvidedStringSetting> {
        private boolean wide;
        private Class<? extends WTextBox.Renderer> renderer;
        private Supplier<String[]> supplier;

        public Builder() {
            super(null);
        }

        public Builder wide() {
            wide = true;
            return this;
        }

        public Builder renderer(Class<? extends WTextBox.Renderer> renderer) {
            this.renderer = renderer;
            return this;
        }

        public Builder supplier(Supplier<String[]> supplier) {
            this.supplier = supplier;
            return this;
        }

        @Override
        public ProvidedStringSetting build() {
            return new ProvidedStringSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, wide, renderer, supplier);
        }
    }
}
