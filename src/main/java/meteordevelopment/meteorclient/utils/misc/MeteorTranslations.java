/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import com.google.gson.Gson;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.utils.PreInit;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.LanguageDefinition;
import net.minecraft.client.resource.language.ReorderingUtil;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.util.Language;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@SuppressWarnings("unused")
public class MeteorTranslations {
    private static final String EN_US_CODE = "en_us";
    private static final Gson GSON = new Gson();
    private static final Map<String, MeteorLanguage> languages = new Object2ObjectOpenHashMap<>();
    private static MeteorLanguage defaultLanguage;

    @PreInit
    public static void preInit() {
        List<String> toLoad = new ArrayList<>(2);
        toLoad.add(EN_US_CODE);
        if (!mc.options.language.equals(EN_US_CODE)) toLoad.add(mc.options.language);

        for (String language : toLoad) {
            loadLanguage(language);
        }

        defaultLanguage = getLanguage(EN_US_CODE);
    }

    public static void loadLanguage(String languageCode) {
        if (languages.containsKey(languageCode)) return;

        LanguageDefinition definition = MinecraftClient.getInstance().getLanguageManager().getLanguage(languageCode);
        if (definition == null) return;

        Object2ObjectOpenHashMap<String, String> languageMap = new Object2ObjectOpenHashMap<>();

        try (InputStream stream = MeteorTranslations.class.getResourceAsStream("/assets/meteor-client/language/" + languageCode + ".json")) {
            if (stream == null) {
                if (languageCode.equals(EN_US_CODE)) throw new RuntimeException("Error loading the default language");
                else MeteorClient.LOG.info("No language file found for '{}'", languageCode);
            }
            else {
                // noinspection unchecked
                Object2ObjectOpenHashMap<String, String> map = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), Object2ObjectOpenHashMap.class);
                languageMap.putAll(map);

                MeteorClient.LOG.info("Loaded language: {}", languageCode);
            }
        } catch (IOException e) {
            if (languageCode.equals(EN_US_CODE)) throw new RuntimeException("Error loading default language", e);
            else MeteorClient.LOG.error("Error loading language: {}", languageCode, e);
        }

        for (MeteorAddon addon : AddonManager.ADDONS) {
            if (addon == MeteorClient.ADDON) continue;

            try (InputStream stream = addon.getClass().getResourceAsStream("/assets/" + addon.id + "/language/" + languageCode + ".json")) {
                if (stream == null) continue;

                // noinspection unchecked
                Object2ObjectOpenHashMap<String, String> map = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), Object2ObjectOpenHashMap.class);
                languageMap.putAll(map);

                MeteorClient.LOG.info("Loaded language {} from addon {}", languageCode, addon.name);
            } catch (IOException e) {
                MeteorClient.LOG.error("Error loading language {} from addon {}", languageCode, addon.name, e);
            }
        }

        if (!languageMap.isEmpty()) {
            languages.put(languageCode, new MeteorLanguage(definition.rightToLeft(), languageMap));
        }
    }

    public static void clearUnusedLanguages(String currentLanguageCode) {
        languages.keySet().removeIf(languageCode -> !languageCode.equals(EN_US_CODE) && !languageCode.equals(currentLanguageCode));
    }

    public static String translate(String key, Object... args) {
        MeteorLanguage currentLang = getCurrentLanguage();
        String translated = currentLang.get(key, getDefaultLanguage().get(key));

        try {
            return String.format(translated, args);
        } catch (IllegalFormatException e) {
            return key;
        }
    }

    public static String translate(String key, String fallback, Object... args) {
        MeteorLanguage currentLang = getCurrentLanguage();
        String translated = currentLang.get(key, getDefaultLanguage().get(key, fallback));

        try {
            return String.format(translated, args);
        } catch (IllegalFormatException e) {
            return fallback;
        }
    }

    public static MeteorLanguage getLanguage(String lang) {
        return languages.get(lang);
    }

    public static MeteorLanguage getCurrentLanguage() {
        return languages.getOrDefault(mc.options.language, getDefaultLanguage());
    }

    public static MeteorLanguage getDefaultLanguage() {
        return defaultLanguage;
    }

    /**
     * @return what percentage of the current language has been localised compared to the default language
     */
    public static double percentLocalised() {
        // Right now there aren't enough differences between the english dialects to justify each having their own
        // translation. Maybe that will change in the future.
        if (isEnglish()) return 100;

        MeteorLanguage currentLang = languages.get(mc.options.language);
        double currentLangSize = currentLang != null ? currentLang.translations.size() : 0;
        return (currentLangSize / getDefaultLanguage().translations.size()) * 100;
    }

    public static boolean isEnglish() {
        return mc.options.language.startsWith("en");
    }

    public static class MeteorLanguage extends Language {
        private final Map<String, String> translations = new Object2ObjectOpenHashMap<>();
        private final boolean rightToLeft;

        public MeteorLanguage(boolean rightToLeft, Map<String, String> translations) {
            this.rightToLeft = rightToLeft;
            this.translations.putAll(translations);
        }

        @Override
        public String get(String key, String fallback) {
            return translations.getOrDefault(key, fallback);
        }

        @Override
        public boolean hasTranslation(String key) {
            return translations.containsKey(key);
        }

        @Override
        public boolean isRightToLeft() {
            return this.rightToLeft;
        }

        @Override
        public OrderedText reorder(StringVisitable text) {
            return ReorderingUtil.reorder(text, this.rightToLeft);
        }
    }
}
