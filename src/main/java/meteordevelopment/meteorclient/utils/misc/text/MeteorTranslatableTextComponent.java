/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc.text;

import com.mojang.serialization.MapCodec;
import meteordevelopment.meteorclient.utils.misc.MeteorTranslations;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.TextContent;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MeteorTranslatableTextComponent implements TextContent {
    private final String key;
    @Nullable
    private final String fallback;
    private final Object[] args;

    private String translation;
    private String cachedLanguage;

    public MeteorTranslatableTextComponent(String key, @Nullable String fallback, Object... args) {
        this.key = key;
        this.fallback = fallback;
        this.args = args;
    }

    public MeteorTranslatableTextComponent(String key, Object... args) {
        this(key, null, args);
    }

    private void updateTranslations() {
        if (!mc.options.language.equals(this.cachedLanguage)) {
            cachedLanguage = mc.options.language;
            translation = fallback == null ? MeteorTranslations.translate(key, args) : MeteorTranslations.translate(key, fallback, args);
        }
    }

    @Override
    public <T> Optional<T> visit(StringVisitable.StyledVisitor<T> visitor, Style style) {
        updateTranslations();

        return visitor.accept(style, translation);
    }

    @Override
    public <T> Optional<T> visit(StringVisitable.Visitor<T> visitor) {
        updateTranslations();

        return visitor.accept(translation);
    }

    @Override
    public MapCodec<? extends TextContent> getCodec() {
        return null;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof MeteorTranslatableTextComponent component)) return false;
        return Objects.equals(this.key, component.key) && Objects.equals(this.fallback, component.fallback) && Arrays.equals(this.args, component.args);
    }

    @Override
    public String toString() {
        return "MeteorTranslatableTextComponent[key=" + key + ", fallback=" + fallback + ", args=" + Arrays.toString(args) + "]";
    }
}
