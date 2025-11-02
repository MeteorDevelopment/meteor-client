/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import com.google.gson.Gson;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import meteordevelopment.meteorclient.MeteorClient;
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

public class MeteorTranslations {
    private static final Gson GSON = new Gson();
    private static Map<String, MeteorLanguage> languages;

    @PreInit
    public static void init() {
        languages = new Object2ObjectOpenHashMap<>();
        List<String> toLoad = new ArrayList<>(2);
        toLoad.add("en_us");
        if (!mc.options.language.equalsIgnoreCase("en_us")) toLoad.add(mc.options.language);

        for (String language : toLoad) {
            loadLanguage(language);
        }
    }

    public static void loadLanguage(String language) {
        language = language.toLowerCase();
        if (languages.containsKey(language)) return;

        try (InputStream stream = MeteorTranslations.class.getResourceAsStream("/assets/meteor-client/language/" + language + ".json")) {
            if (stream == null) {
                if (language.equals("en_us")) throw new RuntimeException("Error loading the default language");
                else MeteorClient.LOG.error("Error loading language: {}", language);
                return;
            }

            // noinspection unchecked
            Object2ObjectOpenHashMap<String, String> map = GSON.fromJson(new InputStreamReader(stream), Object2ObjectOpenHashMap.class);
            languages.put(language, new MeteorLanguage(map));

            MeteorClient.LOG.info("Loaded language: {}", language);
        } catch (IOException e) {
            if (language.equals("en_us")) throw new RuntimeException(e);
            else MeteorClient.LOG.error("Error loading language: {}", language, e);
        }
    }

    public static String translate(String key) {
        if (!key.startsWith("meteor.")) key = "meteor." + key;
        return _translate(key);
    }

    private static String _translate(String key) {
        MeteorLanguage currentLang = getCurrentLanguage();
        return currentLang.hasTranslation(key) ? currentLang.get(key) : getDefaultLanguage().get(key);
    }

    public static MeteorLanguage getLanguage(String lang) {
        return languages.get(lang);
    }

    public static MeteorLanguage getCurrentLanguage() {
        return languages.get(mc.options.language.toLowerCase());
    }

    public static MeteorLanguage getDefaultLanguage() {
        return languages.get("en_us");
    }

    public static class MeteorLanguage extends Language {
        private final Map<String, String> translations;

        public MeteorLanguage(Map<String, String> translations) {
            this.translations = translations;
        }

        @Override
        public String get(String key, String fallback) {
            return translations.getOrDefault(key, fallback);
        }

        @Override
        public boolean hasTranslation(String key){
            return translations.containsKey(key);
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
