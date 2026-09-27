/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.translation;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Produces Minecraft {@link Component} objects containing the ALREADY-TRANSLATED literal string.
 *
 * <p>CRITICAL: This class must NEVER produce {@code Component.translatable()} with a Meteor or addon
 * key. {@code Component.translatable()} serializes the key (not the resolved text) into
 * server-bound packets — that is the vulnerability this system is designed to prevent. All methods
 * return {@code Component.literal(translatedString)}.
 *
 * <p>The server receives only the final translated text, e.g. "Fly" — never the key
 * {@code "meteor-client.module.fly.name"}.
 */
public final class MeteorText {

    private MeteorText() {}

    /** Translate a key and return a literal Component. Safe for all Minecraft APIs. */
    public static MutableComponent translated(TranslationKey key) {
        return Component.literal(key.get());
    }

    /** Translate a key with arguments and return a literal Component. */
    public static MutableComponent translated(TranslationKey key, Object... args) {
        return Component.literal(key.get(args));
    }

    /** Translate by raw key string (convenience overload). */
    public static MutableComponent translated(String rawKey) {
        return Component.literal(TranslationManager.get(rawKey));
    }

    /** Translate by raw key string with arguments. */
    public static MutableComponent translated(String rawKey, Object... args) {
        return Component.literal(TranslationManager.get(rawKey, args));
    }

    /** Translate and apply ChatFormatting. */
    public static MutableComponent translated(TranslationKey key, ChatFormatting... formattings) {
        return Component.literal(key.get()).withStyle(formattings);
    }

    /** Translate and apply a Style. */
    public static MutableComponent translated(TranslationKey key, Style style) {
        return Component.literal(key.get()).setStyle(style);
    }

    // ------------------------------------------------------------------
    // Builder — for UI components that retain the key for re-resolution
    // ------------------------------------------------------------------

    public static Builder builder(TranslationKey key) {
        return new Builder(key);
    }

    public static final class Builder {
        private final TranslationKey key;
        private Style style = Style.EMPTY;
        private Object[] args = null;

        private Builder(TranslationKey key) {
            this.key = key;
        }

        public Builder style(Style style) {
            this.style = style;
            return this;
        }

        public Builder formatting(ChatFormatting... f) {
            this.style = Style.EMPTY.applyFormats(f);
            return this;
        }

        public Builder args(Object... args) {
            this.args = args;
            return this;
        }

        public MutableComponent build() {
            String text = args != null ? key.get(args) : key.get();
            return Component.literal(text).setStyle(style);
        }
    }
}
