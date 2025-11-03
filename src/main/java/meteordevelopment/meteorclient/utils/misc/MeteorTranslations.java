/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import com.google.gson.Gson;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.utils.PreInit;
import net.minecraft.client.resource.language.ReorderingUtil;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.util.Language;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@SuppressWarnings("unused")
public class MeteorTranslations {
    private static final Gson GSON = new Gson();
    private static final Map<String, MeteorLanguage> languages = new Object2ObjectOpenHashMap<>();

    @PreInit
    public static void preInit() {
        List<String> toLoad = new ArrayList<>(2);
        toLoad.add("en_us");
        if (!mc.options.language.equalsIgnoreCase("en_us")) toLoad.add(mc.options.language);

        for (String language : toLoad) {
            loadLanguage(language);
        }
    }

    public static void loadLanguage(String languageCode) {
        languageCode = languageCode.toLowerCase();
        if (languages.containsKey(languageCode)) return;

        try (InputStream stream = MeteorTranslations.class.getResourceAsStream("/assets/meteor-client/language/" + languageCode + ".json")) {
            if (stream == null) {
                if (languageCode.equals("en_us")) throw new RuntimeException("Error loading the default language");
                else MeteorClient.LOG.info("No language file found for '{}'", languageCode);
            }
            else {
                // noinspection unchecked
                Object2ObjectOpenHashMap<String, String> map = GSON.fromJson(new InputStreamReader(stream), Object2ObjectOpenHashMap.class);
                languages.put(languageCode, new MeteorLanguage(map));

                MeteorClient.LOG.info("Loaded language: {}", languageCode);
            }
        } catch (IOException e) {
            if (languageCode.equals("en_us")) throw new RuntimeException(e);
            else MeteorClient.LOG.error("Error loading language: {}", languageCode, e);
        }

        for (MeteorAddon addon : AddonManager.ADDONS) {
            if (addon == MeteorClient.ADDON) continue;

            try (InputStream stream = addon.provideLanguage(languageCode)) {
                if (stream == null) continue;
                MeteorLanguage lang = languages.getOrDefault(languageCode, new MeteorLanguage());

                // noinspection unchecked
                Object2ObjectOpenHashMap<String, String> map = GSON.fromJson(new InputStreamReader(stream), Object2ObjectOpenHashMap.class);
                lang.addCustomTranslation(map);
                languages.put(languageCode, lang);

                MeteorClient.LOG.info("Loaded language {} from addon {}", languageCode, addon.name);
            } catch (IOException e) {
                MeteorClient.LOG.error("Error loading language {} from addon {}", languageCode, addon.name, e);
            }
        }
    }

    public static String translate(String key) {
        MeteorLanguage currentLang = getCurrentLanguage();
        return currentLang.hasTranslation(key) ? currentLang.get(key) : getDefaultLanguage().get(key);
    }

    public static MeteorLanguage getLanguage(String lang) {
        return languages.get(lang);
    }

    public static MeteorLanguage getCurrentLanguage() {
        return languages.getOrDefault(mc.options.language.toLowerCase(), getDefaultLanguage());
    }

    public static MeteorLanguage getDefaultLanguage() {
        return languages.get("en_us");
    }

    public static class MeteorLanguage extends Language {
        private final Map<String, String> translations;
        private final List<Map<String, String>> customTranslations = new ObjectArrayList<>();

        public MeteorLanguage() {
            this.translations = new Object2ObjectOpenHashMap<>();
        }

        public MeteorLanguage(Map<String, String> translations) {
            this.translations = translations;
        }

        public void addCustomTranslation(Map<String, String> customTranslation) {
            if (customTranslations.contains(customTranslation)) return;

            customTranslations.add(customTranslation);
        }

        @Override
        public String get(String key, String fallback) {
            if (translations.containsKey(key)) return translations.get(key);

            for (Map<String, String> customTranslation : customTranslations) {
                if (customTranslation.containsKey(key)) return customTranslation.get(key);
            }

            return fallback;
        }

        @Override
        public boolean hasTranslation(String key) {
            if (translations.containsKey(key)) return true;

            for (Map<String, String> customTranslation : customTranslations) {
                if (customTranslation.containsKey(key)) return true;
            }

            return false;
        }

        @Override
        public boolean isRightToLeft() {
            return false;
        }

        @Override
        public OrderedText reorder(StringVisitable text) {
            return ReorderingUtil.reorder(text, false);
        }
    }
}
