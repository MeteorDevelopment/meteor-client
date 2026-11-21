/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.translation;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenience helper for addon developers.
 *
 * <pre>
 * // In MeteorAddon.onInitialize():
 * Translations.register(this);   // auto-detects mod ID from FabricLoader
 *
 * // In module constructors:
 * TranslationKey.of("my-addon", "module.example.name")
 * // → "my-addon.module.example.name"
 * </pre>
 */
public final class Translations {

    private static final Logger LOGGER = LoggerFactory.getLogger("MeteorTranslations/Addon");

    private Translations() {}

    /**
     * Register an addon's translation namespace.
     * The mod ID is resolved via FabricLoader from the addon's class.
     *
     * @param addon any instance from the addon (typically "this" in onInitialize)
     */
    public static void register(Object addon) {
        String modId = resolveModId(addon.getClass());
        TranslationManager.registerNamespace(modId);
        LOGGER.info("Addon translation namespace registered: {} (from {})",
            modId, addon.getClass().getSimpleName());
    }

    /**
     * Register an addon by explicit mod ID.
     * Use this when automatic detection from the class path does not work.
     */
    public static void register(String modId) {
        TranslationManager.registerNamespace(modId);
    }

    /**
     * Create a TranslationKey in the given namespace.
     *
     * @param namespace the addon's mod ID, e.g. "trouser-streak"
     * @param subKey    key without namespace, e.g. "module.fly.name"
     */
    public static TranslationKey key(String namespace, String subKey) {
        return TranslationKey.of(namespace, subKey);
    }

    /**
     * Create a TranslationKey using all four parts explicitly.
     *
     * @param namespace mod ID
     * @param type      module | setting | command | hud | etc.
     * @param id        kebab-case identifier
     * @param attribute name | description | tooltip | etc.
     */
    public static TranslationKey key(String namespace, String type, String id, String attribute) {
        return TranslationKey.of(namespace + "." + type + "." + id + "." + attribute);
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    static String resolveModId(Class<?> clazz) {
        String pkg = clazz.getPackageName();
        for (ModContainer container : FabricLoader.getInstance().getAllMods()) {
            String modId = container.getMetadata().getId();
            // Match: package contains the mod id (with or without hyphens)
            String flatId = modId.replace("-", "").replace("_", "");
            String flatPkg = pkg.replace("-", "").replace("_", "").replace(".", "");
            if (flatPkg.contains(flatId) && !modId.equals("minecraft") && !modId.equals("java")) {
                return modId;
            }
        }
        // Last resort: third package segment, hyphens substituted
        String[] parts = pkg.split("\\.");
        return parts.length >= 3 ? parts[2].replace("_", "-") : pkg;
    }
}
