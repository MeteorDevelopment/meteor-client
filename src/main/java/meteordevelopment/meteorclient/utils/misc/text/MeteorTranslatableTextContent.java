/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc.text;

import meteordevelopment.meteorclient.utils.misc.MeteorTranslations;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MeteorTranslatableTextContent extends RichTextContent {
    private final String key;
    private final @Nullable String fallback;

    private String cachedLanguage;

    public MeteorTranslatableTextContent(String key, @Nullable String fallback, Object... args) {
        super(args);
        this.key = key;
        this.fallback = fallback;
    }

    public MeteorTranslatableTextContent(String key, Object... args) {
        this(key, null, args);
    }

    @Override
    protected boolean shouldUpdate() {
        return !mc.options.language.equals(this.cachedLanguage);
    }

    @Override
    protected void update(String template) {
        cachedLanguage = mc.options.language;
        super.update(template);
    }

    @Override
    protected String getTemplate() {
        return fallback == null ? MeteorTranslations.translate(key) : MeteorTranslations.translate(key, fallback);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof MeteorTranslatableTextContent component)) return false;
        return Objects.equals(this.key, component.key) && Objects.equals(this.fallback, component.fallback) && Arrays.equals(this.args, component.args);
    }

    @Override
    public String toString() {
        return "MeteorTranslatableTextContent[key=" + key + ", fallback=" + fallback + ", args=" + Arrays.toString(args) + "]";
    }
}
