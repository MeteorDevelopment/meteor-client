/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.translation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime registry of all TranslationKey instances created during the session.
 * Used by the Gradle validation tooling and duplicate detection.
 *
 * <p>Registration happens automatically inside {@link TranslationKey#of(String)}.
 */
public final class TranslationRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger("MeteorTranslations/Registry");

    private static final Set<String> keys = ConcurrentHashMap.newKeySet();
    private static final Map<String, Integer> counts = new ConcurrentHashMap<>();

    private TranslationRegistry() {}

    /** Called automatically by the TranslationKey constructor. Do not call directly. */
    static void register(String key) {
        keys.add(key);
        int n = counts.merge(key, 1, Integer::sum);
        if (n > 1) {
            LOGGER.warn("Duplicate TranslationKey registration (×{}): \"{}\"", n, key);
        }
    }

    public static Set<String> allKeys() {
        return Collections.unmodifiableSet(keys);
    }

    public static boolean isRegistered(String key) {
        return keys.contains(key);
    }

    /** Keys registered more than once — candidates for deduplication review. */
    public static Map<String, Integer> getDuplicateCandidates() {
        Map<String, Integer> result = new HashMap<>();
        counts.forEach((k, v) -> {
            if (v > 1) result.put(k, v);
        });
        return result;
    }
}
