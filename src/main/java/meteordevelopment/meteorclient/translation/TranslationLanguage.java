/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.translation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * An immutable in-memory snapshot of one parsed language file (e.g. de_de.json).
 */
public final class TranslationLanguage {

    private final String code;
    private final Map<String, String> entries;

    TranslationLanguage(String code, Map<String, String> entries) {
        this.code = code;
        this.entries = Collections.unmodifiableMap(new HashMap<>(entries));
    }

    /** The locale code, e.g. "de_de". */
    public String code() {
        return code;
    }

    /** Returns the translated value, or null if the key is absent. */
    public String get(String key) {
        return entries.get(key);
    }

    public boolean has(String key) {
        return entries.containsKey(key);
    }

    public Set<String> keys() {
        return entries.keySet();
    }

    public int size() {
        return entries.size();
    }
}
