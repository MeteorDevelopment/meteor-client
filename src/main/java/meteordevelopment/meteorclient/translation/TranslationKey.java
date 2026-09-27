/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.translation;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A typed, validated reference to a translation key.
 *
 * <p>UI components MUST retain TranslationKey instances and resolve them lazily
 * via {@link #get()} at render time — not at construction time — so that runtime
 * language switches are reflected without rebuilding components.
 *
 * <p>Keys follow the schema: {@code namespace.type.identifier.attribute}.
 * All segments: lowercase, kebab-case, no spaces, no underscores.
 * Example: {@code meteor-client.module.fly.name}
 */
public final class TranslationKey {

    private static final Pattern VALID = Pattern.compile(
        "^[a-z0-9][a-z0-9-]*(?:\\.[a-z0-9][a-z0-9-]*){1,}$"
    );

    private final String key;

    private TranslationKey(String key) {
        this.key = key;
        TranslationRegistry.register(key);
    }

    /**
     * Create a TranslationKey from a full key string.
     * Throws IllegalArgumentException if the format is invalid.
     *
     * @param key e.g. "meteor-client.module.fly.name"
     */
    public static TranslationKey of(String key) {
        Objects.requireNonNull(key, "key must not be null");
        if (!VALID.matcher(key).matches()) {
            throw new IllegalArgumentException(
                "Invalid translation key: \"" + key + "\". " +
                "Keys must be all lowercase, kebab-case segments, separated by dots. " +
                "Example: meteor-client.module.fly-speed.name"
            );
        }
        return new TranslationKey(key);
    }

    /**
     * Create a TranslationKey within a namespace.
     * Equivalent to {@code TranslationKey.of(namespace + "." + subKey)}.
     *
     * @param namespace mod ID, e.g. "trouser-streak"
     * @param subKey    key without namespace, e.g. "module.fly.name"
     */
    public static TranslationKey of(String namespace, String subKey) {
        return of(namespace + "." + subKey);
    }

    /** Resolve to the translated string in the current language. */
    public String get() {
        return TranslationManager.get(key);
    }

    /** Resolve with printf-style %s/%d arguments. */
    public String get(Object... args) {
        return TranslationManager.get(key, args);
    }

    /** Returns the raw key string — never a translated value. */
    public String raw() {
        return key;
    }

    @Override
    public String toString() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TranslationKey tk)) return false;
        return key.equals(tk.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
