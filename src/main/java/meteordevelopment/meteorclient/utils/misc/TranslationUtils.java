/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.ResourcePacksReloadedEvent;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class TranslationUtils {
    private static final Map<String, Set<String>> namespaceTranslations = new HashMap<>();

    private TranslationUtils() {
    }

    @PreInit
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(TranslationUtils.class);
    }

    @EventHandler
    private static void onResourcePacksReloaded(ResourcePacksReloadedEvent event) {
        namespaceTranslations.clear();
    }

    public static boolean isNamespaceTranslation(String namespace, String text) {
        return namespaceTranslations.computeIfAbsent(namespace, TranslationUtils::loadNamespaceTranslations).contains(text);
    }

    private static Set<String> loadNamespaceTranslations(String namespace) {
        Set<String> translations = new HashSet<>();

        mc.getResourceManager()
            .listResources("lang", id -> id.getNamespace().equals(namespace) && id.getPath().endsWith(".json"))
            .values()
            .forEach(resource -> readTranslations(namespace, resource, translations));

        return translations;
    }

    private static void readTranslations(String namespace, Resource resource, Set<String> translations) {
        try (var reader = resource.openAsReader()) {
            JsonElement json = JsonParser.parseReader(reader);
            if (!json.isJsonObject()) return;

            for (String key : json.getAsJsonObject().keySet()) {
                translations.add(I18n.get(key));
            }
        } catch (IOException | JsonParseException e) {
            MeteorClient.LOG.warn("Failed to read translations for namespace '{}'.", namespace, e);
        }
    }
}
