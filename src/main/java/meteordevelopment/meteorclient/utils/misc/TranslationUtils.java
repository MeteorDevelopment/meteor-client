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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class TranslationUtils {
    private static final Map<String, Set<String>> namespaceTranslationKeys = new HashMap<>();
    private static final Map<String, Set<String>> namespaceTranslations = new HashMap<>();

    private TranslationUtils() {
    }

    @PreInit
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(TranslationUtils.class);
    }

    @EventHandler
    private static void onResourcePacksReloaded(ResourcePacksReloadedEvent event) {
        namespaceTranslationKeys.clear();
        namespaceTranslations.clear();
    }

    public static boolean isNamespaceTranslation(String namespace, String text) {
        return namespaceTranslations.computeIfAbsent(namespace, TranslationUtils::loadNamespaceTranslations).contains(text);
    }

    public static String getNamespaceTranslationFallback(String namespace, Component component) {
        if (!containsNamespaceTranslation(namespace, component)) return null;

        if (component.getContents() instanceof TranslatableContents contents) {
            return contents.getFallback() != null ? contents.getFallback() : contents.getKey();
        }

        for (Component sibling : component.getSiblings()) {
            String fallback = getNamespaceTranslationFallback(namespace, sibling);
            if (fallback != null) return fallback;
        }

        return null;
    }

    private static boolean containsNamespaceTranslation(String namespace, Component component) {
        if (component.getContents() instanceof TranslatableContents contents) {
            if (getNamespaceTranslationKeys(namespace).contains(contents.getKey())) return true;

            for (Object arg : contents.getArgs()) {
                if (arg instanceof Component argComponent && containsNamespaceTranslation(namespace, argComponent)) return true;
            }
        }

        for (Component sibling : component.getSiblings()) {
            if (containsNamespaceTranslation(namespace, sibling)) return true;
        }

        return false;
    }

    private static Set<String> loadNamespaceTranslations(String namespace) {
        Set<String> translations = new HashSet<>();

        for (String key : getNamespaceTranslationKeys(namespace)) {
            translations.add(I18n.get(key));
        }

        return translations;
    }

    private static Set<String> getNamespaceTranslationKeys(String namespace) {
        return namespaceTranslationKeys.computeIfAbsent(namespace, TranslationUtils::loadNamespaceTranslationKeys);
    }

    private static Set<String> loadNamespaceTranslationKeys(String namespace) {
        Set<String> keys = new HashSet<>();

        mc.getResourceManager()
            .listResources("lang", id -> id.getNamespace().equals(namespace) && id.getPath().endsWith(".json"))
            .values()
            .forEach(resource -> readTranslationKeys(namespace, resource, keys));

        return keys;
    }

    private static void readTranslationKeys(String namespace, Resource resource, Set<String> keys) {
        try (var reader = resource.openAsReader()) {
            JsonElement json = JsonParser.parseReader(reader);
            if (!json.isJsonObject()) return;

            keys.addAll(json.getAsJsonObject().keySet());
        } catch (IOException | JsonParseException e) {
            MeteorClient.LOG.warn("Failed to read translation keys for namespace '{}'.", namespace, e);
        }
    }
}
