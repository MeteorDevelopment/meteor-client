/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ProvidedStringSetting extends StringSetting {
    public final Supplier<String[]> supplier;

    public ProvidedStringSetting(String name, String description, String defaultValue, Consumer<String> onChanged, Consumer<Setting<String>> onModuleActivated, IVisible visible, Supplier<String[]> supplier) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        this.supplier = supplier;
    }

    public static class Builder extends SettingBuilder<Builder, String, ProvidedStringSetting> {
        private Supplier<String[]> supplier;

        public Builder() {
            super(null);
        }

        public Builder supplier(Supplier<String[]> supplier) {
            this.supplier = supplier;
            return this;
        }

        @Override
        public ProvidedStringSetting build() {
            return new ProvidedStringSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, supplier);
        }
    }
}
