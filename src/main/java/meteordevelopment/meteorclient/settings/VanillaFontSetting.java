/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.function.Consumer;

public class VanillaFontSetting extends Setting<Identifier> {
    public VanillaFontSetting(String name, String description, Identifier defaultValue, Consumer<Identifier> onChanged, Consumer<Setting<Identifier>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    protected Identifier parseImpl(String str) {
        return Identifier.tryParse(str);
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return List.of(Style.DEFAULT_FONT_ID);
    }

    @Override
    protected boolean isValueValid(Identifier value) {
        return true;
    }

    @Override
    protected NbtCompound save(NbtCompound tag) {
        tag.putString("id", get().toString());
        return tag;
    }

    @Override
    protected Identifier load(NbtCompound tag) {
        set(Identifier.tryParse(tag.getString("id")));
        return get();
    }

    public static class Builder extends SettingBuilder<Builder, Identifier, VanillaFontSetting> {
        public Builder() {
            super(Style.DEFAULT_FONT_ID);
        }

        @Override
        public VanillaFontSetting build() {
            return new VanillaFontSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
