/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import meteordevelopment.meteorclient.renderer.Fonts;
import meteordevelopment.meteorclient.renderer.text.FontFace;
import meteordevelopment.meteorclient.renderer.text.FontFamily;
import meteordevelopment.meteorclient.renderer.text.FontInfo;
import net.minecraft.nbt.NbtCompound;

import java.util.List;
import java.util.function.Consumer;

public class FontFaceSetting extends Setting<FontFace> {
    public FontFaceSetting(String name, String description, FontFace defaultValue, Consumer<FontFace> onChanged, Consumer<Setting<FontFace>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    protected FontFace parseImpl(String str) {
        String[] split = str.replace(" ", "").split("-");
        if (split.length != 2) return null;

        for (FontFamily family : Fonts.FONT_FAMILIES) {
            if (family.getName().replace(" ", "").equals(split[0])) {
                try {
                    return family.get(FontInfo.Type.valueOf(split[1]));
                }
                catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }

        return null;
    }

    @Override
    public List<String> getSuggestions() {
        return List.of("JetBrainsMono-Regular", "Arial-Bold");
    }

    @Override
    protected boolean isValueValid(FontFace value) {
        if (value == null) return false;

        for (FontFamily fontFamily : Fonts.FONT_FAMILIES) {
            if (fontFamily.hasType(value.info.type())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected NbtCompound save(NbtCompound tag) {
        tag.putString("family", get().info.family());
        tag.putString("type", get().info.type().toString());
        return tag;
    }

    @Override
    protected FontFace load(NbtCompound tag) {
        String family = tag.getString("family");
        FontInfo.Type type;

        try {
            type = FontInfo.Type.valueOf(tag.getString("type"));
        }
        catch (IllegalArgumentException ignored) {
            set(Fonts.DEFAULT_FONT);
            return get();
        }

        boolean changed = false;
        for (FontFamily fontFamily : Fonts.FONT_FAMILIES) {
            if (fontFamily.getName().equals(family)) {
                set(fontFamily.get(type));
                changed = true;
            }
        }
        if (!changed) set(Fonts.DEFAULT_FONT);

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, FontFace, FontFaceSetting> {
        public Builder() {
            super(Fonts.DEFAULT_FONT);
        }

        @Override
        public FontFaceSetting build() {
            return new FontFaceSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
