/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.translation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central manager for all Meteor and addon translations.
 *
 * <p>Usage:
 * <pre>
 *   TranslationManager.get("meteor-client.module.fly.name");
 *   TranslationManager.get("meteor-client.module.enabled", module.name());
 *   TranslationManager.setLanguage("de_de");
 * </pre>
 *
 * <p>Lookup order: current language → English fallback → raw key (with warning).
 */
public final class TranslationManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("MeteorTranslations");

    public static final String METEOR_MOD_ID = "meteor-client";
    public static final String FALLBACK_LANG = "en_us";

    private static volatile String currentLang = FALLBACK_LANG;

    /** namespace → langCode → TranslationLanguage */
    private static final Map<String, Map<String, TranslationLanguage>> cache = new ConcurrentHashMap<>();

    private static final Set<String> registeredNamespaces = ConcurrentHashMap.newKeySet();
    private static final List<Runnable> changeListeners = Collections.synchronizedList(new ArrayList<>());

    static {
        registerNamespace(METEOR_MOD_ID);
    }

    private TranslationManager() {}

    // -------------------------------------------------------------------------
    // Core API
    // -------------------------------------------------------------------------

    /**
     * Look up a translation key in the current language, falling back to English.
     *
     * @param key full translation key, e.g. "meteor-client.module.fly.name"
     * @return translated string, English fallback, or the raw key with a logged warning
     */
    public static String get(String key) {
        String namespace = namespaceOf(key);

        String result = lookup(namespace, currentLang, key);
        if (result != null) return result;

        if (!currentLang.equals(FALLBACK_LANG)) {
            result = lookup(namespace, FALLBACK_LANG, key);
            if (result != null) return result;
        }

        LOGGER.warn("Missing translation key: \"{}\" (lang={})", key, currentLang);
        return key;
    }

    /**
     * Look up a key and format with printf-style arguments.
     *
     * @param key  full translation key
     * @param args values for %s, %d, %f placeholders
     */
    public static String get(String key, Object... args) {
        String template = get(key);
        if (args == null || args.length == 0) return template;
        try {
            return String.format(template, args);
        } catch (Exception e) {
            LOGGER.warn("Placeholder mismatch for key \"{}\": {}", key, e.getMessage());
            return template;
        }
    }

    /**
     * Switch the active language. Does not require a restart.
     * Notifies all registered change listeners.
     *
     * @param langCode BCP-47-style locale code, e.g. "de_de"
     */
    public static void setLanguage(String langCode) {
        Objects.requireNonNull(langCode);
        String normalized = langCode.toLowerCase(Locale.ROOT);
        if (!normalized.equals(currentLang)) {
            String old = currentLang;
            currentLang = normalized;
            LOGGER.info("Language switched: {} → {}", old, normalized);
            invalidateAndNotify();
        }
    }

    /** Returns the currently active language code. */
    public static String getCurrentLanguage() {
        return currentLang;
    }

    /**
     * Register an addon namespace. Must be called from MeteorAddon.onInitialize().
     * The modId must exactly match the id in fabric.mod.json and the assets/ directory name.
     */
    public static void registerNamespace(String modId) {
        Objects.requireNonNull(modId);
        if (registeredNamespaces.add(modId)) {
            LOGGER.info("Translation namespace registered: {}", modId);
            loadIntoCache(modId, FALLBACK_LANG);  // pre-warm English immediately
        }
    }

    /**
     * Returns all locale codes available across all registered namespaces
     * (the union of every namespace's discovered locales).
     */
    public static List<String> getAvailableLanguages() {
        Set<String> langs = new LinkedHashSet<>();
        langs.add(FALLBACK_LANG);
        for (String ns : registeredNamespaces) {
            langs.addAll(TranslationLoader.discoverLocales(ns));
        }
        return new ArrayList<>(langs);
    }

    /**
     * Register a callback to be invoked when the language changes.
     * Use this in UI components to invalidate cached display text.
     */
    public static void addChangeListener(Runnable listener) {
        changeListeners.add(listener);
    }

    public static void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private static String lookup(String namespace, String langCode, String key) {
        Map<String, TranslationLanguage> nsCache = cache.get(namespace);
        if (nsCache == null) return null;

        TranslationLanguage lang = nsCache.get(langCode);
        if (lang == null) {
            lang = loadIntoCache(namespace, langCode);
        }
        return lang != null ? lang.get(key) : null;
    }

    private static TranslationLanguage loadIntoCache(String namespace, String langCode) {
        Optional<TranslationLanguage> loaded = TranslationLoader.load(namespace, langCode);
        loaded.ifPresent(l ->
            cache.computeIfAbsent(namespace, k -> new ConcurrentHashMap<>())
                .put(langCode, l)
        );
        return loaded.orElse(null);
    }

    private static String namespaceOf(String key) {
        int dot = key.indexOf('.');
        return dot < 0 ? key : key.substring(0, dot);
    }

    // -------------------------------------------------------------------------
    // Key derivation — canonical Meteor key schema
    //
    // Meteor's module and setting names are already kebab-case identifiers used as
    // config keys, so keys are derived centrally from those raw names rather than
    // hardcoded at every call site. This keeps keys stable, collision-free and
    // requires no per-site edits for the 3000+ settings.
    // -------------------------------------------------------------------------

    public static final String TYPE_CATEGORY = "category";
    public static final String TYPE_MODULE = "module";
    public static final String TYPE_SETTING = "setting";
    public static final String TYPE_HUD = "hud";
    public static final String TYPE_COMMAND = "command";

    /** Scope used for settings that are not owned by a module or HUD element. */
    public static final String GLOBAL_SCOPE = "global";

    /** Lowercase and replace any character that is not valid in a key segment with '-'. */
    public static String slug(String raw) {
        if (raw == null) return "";
        String s = raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        s = s.replaceAll("^-+", "").replaceAll("-+$", "");
        return s.isEmpty() ? "unnamed" : s;
    }

    public static String categoryNameKey(String categoryName) {
        return METEOR_MOD_ID + "." + TYPE_CATEGORY + "." + slug(categoryName) + ".name";
    }

    public static String moduleNameKey(String namespace, String moduleId) {
        return namespace + "." + TYPE_MODULE + "." + slug(moduleId) + ".name";
    }

    public static String moduleDescriptionKey(String namespace, String moduleId) {
        return namespace + "." + TYPE_MODULE + "." + slug(moduleId) + ".description";
    }

    public static String moduleNameKey(String moduleId) {
        return moduleNameKey(METEOR_MOD_ID, moduleId);
    }

    public static String moduleDescriptionKey(String moduleId) {
        return moduleDescriptionKey(METEOR_MOD_ID, moduleId);
    }

    public static String hudNameKey(String namespace, String hudId) {
        return namespace + "." + TYPE_HUD + "." + slug(hudId) + ".name";
    }

    public static String hudDescriptionKey(String namespace, String hudId) {
        return namespace + "." + TYPE_HUD + "." + slug(hudId) + ".description";
    }

    public static String hudNameKey(String hudId) {
        return METEOR_MOD_ID + "." + TYPE_HUD + "." + slug(hudId) + ".name";
    }

    public static String hudDescriptionKey(String hudId) {
        return METEOR_MOD_ID + "." + TYPE_HUD + "." + slug(hudId) + ".description";
    }

    public static String commandNameKey(String namespace, String commandId) {
        return namespace + "." + TYPE_COMMAND + "." + slug(commandId) + ".name";
    }

    public static String commandDescriptionKey(String namespace, String commandId) {
        return namespace + "." + TYPE_COMMAND + "." + slug(commandId) + ".description";
    }

    public static String commandNameKey(String commandId) {
        return commandNameKey(METEOR_MOD_ID, commandId);
    }

    public static String commandDescriptionKey(String commandId) {
        return commandDescriptionKey(METEOR_MOD_ID, commandId);
    }

    /**
     * Setting key, scoped by namespace, owner and group so that two owners — or two groups
     * within one owner — may reuse a setting name (e.g. "mode") with different display text.
     *
     * @param namespace translation namespace, i.e. the mod id (e.g. "meteor-client", "gui-plus")
     * @param ownerId   owner id — module name, HUD element name, or {@link #GLOBAL_SCOPE}
     * @param groupId   the setting group's raw name, e.g. "Anti Kick"
     * @param name      the setting's raw name (already kebab-case)
     */
    public static String settingNameKey(String namespace, String ownerId, String groupId, String name) {
        return namespace + "." + TYPE_SETTING + "." + slug(ownerId) + "." + slug(groupId) + "." + slug(name) + ".name";
    }

    public static String settingDescriptionKey(String namespace, String ownerId, String groupId, String name) {
        return namespace + "." + TYPE_SETTING + "." + slug(ownerId) + "." + slug(groupId) + "." + slug(name) + ".description";
    }

    public static String settingNameKey(String ownerId, String groupId, String name) {
        return settingNameKey(METEOR_MOD_ID, ownerId, groupId, name);
    }

    public static String settingDescriptionKey(String ownerId, String groupId, String name) {
        return settingDescriptionKey(METEOR_MOD_ID, ownerId, groupId, name);
    }

    private static void invalidateAndNotify() {
        // Drop the newly-selected language from cache so it reloads fresh
        for (String ns : registeredNamespaces) {
            Map<String, TranslationLanguage> nsCache = cache.get(ns);
            if (nsCache != null) nsCache.remove(currentLang);
        }
        List<Runnable> snapshot = new ArrayList<>(changeListeners);
        for (Runnable l : snapshot) {
            try {
                l.run();
            } catch (Exception e) {
                LOGGER.warn("Change listener error", e);
            }
        }
    }
}
