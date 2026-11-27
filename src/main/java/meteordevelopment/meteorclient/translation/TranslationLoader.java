/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.translation;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Discovers and parses language JSON files from the mod classpath.
 * Handles both meteor-client's own lang/ directory and all addon lang/ directories.
 */
public final class TranslationLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger("MeteorTranslations/Loader");
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    /** All locale codes the loader will probe when discovering available languages. */
    private static final List<String> PROBE_LOCALES = List.of(
        "en_us", "en_gb", "de_de", "fr_fr", "es_es", "pt_br", "zh_cn", "zh_tw",
        "ja_jp", "ko_kr", "ru_ru", "pl_pl", "it_it", "nl_nl", "hi_in",
        "tr_tr", "uk_ua", "cs_cz", "hu_hu", "sv_se", "fi_fi", "da_dk",
        "nb_no", "ro_ro", "bg_bg", "el_gr", "ar_sa", "he_il", "vi_vn",
        "th_th", "id_id"
    );

    private TranslationLoader() {}

    /**
     * Load a language file for a given mod ID and locale code.
     *
     * @param modId    mod/addon id — matches assets/&lt;modId&gt;/lang/
     * @param langCode locale code, e.g. "de_de"
     * @return parsed TranslationLanguage, or empty if the file does not exist
     */
    public static Optional<TranslationLanguage> load(String modId, String langCode) {
        String resourcePath = "assets/" + modId + "/lang/" + langCode + ".json";

        // Preferred: Fabric mod container (correct for mods on classpath)
        Optional<ModContainer> container = FabricLoader.getInstance().getModContainer(modId);
        if (container.isPresent()) {
            try {
                var found = container.get().findPath(resourcePath);
                if (found.isPresent()) {
                    try (InputStream in = Files.newInputStream(found.get())) {
                        return Optional.of(parse(langCode, in));
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Error loading lang via mod container [{}/{}]: {}", modId, langCode, e.getMessage());
            }
        }

        // Fallback: raw classloader resource (works during testing / dev)
        InputStream fallback = TranslationLoader.class.getClassLoader().getResourceAsStream(resourcePath);
        if (fallback != null) {
            return Optional.of(parse(langCode, fallback));
        }

        return Optional.empty();
    }

    /**
     * Return all locale codes available for a given mod ID by probing known codes.
     */
    public static List<String> discoverLocales(String modId) {
        List<String> available = new ArrayList<>();
        for (String code : PROBE_LOCALES) {
            if (load(modId, code).isPresent()) available.add(code);
        }
        return available;
    }

    private static TranslationLanguage parse(String code, InputStream stream) {
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            Map<String, String> map = GSON.fromJson(reader, MAP_TYPE);
            return new TranslationLanguage(code, map != null ? map : new HashMap<>());
        } catch (Exception e) {
            LOGGER.error("Failed to parse language file for locale '{}': {}", code, e.getMessage());
            return new TranslationLanguage(code, new HashMap<>());
        }
    }
}
