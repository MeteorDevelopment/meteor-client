/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.addons;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.translation.TranslationManager;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;

import java.util.ArrayList;
import java.util.List;

public class AddonManager {
    public static final List<MeteorAddon> ADDONS = new ArrayList<>();

    /**
     * Namespace of the addon whose {@code onInitialize()} is currently running. Settings that
     * are not attached to a module or HUD element fall back to this namespace during addon
     * initialization, so their keys stay inside the addon's own language files.
     */
    private static volatile String initializingNamespace = null;

    public static String initializingNamespace() {
        return initializingNamespace;
    }

    /** Run an addon's initializer with its namespace set as the ambient namespace. */
    public static void initialize(MeteorAddon addon) {
        initializingNamespace = addon.modId != null ? addon.modId : MeteorClient.MOD_ID;
        try {
            addon.onInitialize();
        } finally {
            initializingNamespace = null;
        }
    }

    /**
     * Resolve the translation namespace (mod id) that owns the given class, based on the
     * package of the registered addons. Classes under no addon package — i.e. core Meteor
     * classes — resolve to {@code meteor-client}.
     */
    public static String namespaceOf(Class<?> clazz) {
        if (clazz != null) {
            String classname = clazz.getName();
            for (MeteorAddon addon : ADDONS) {
                if (classname.startsWith(addon.getPackage())) {
                    return addon.modId != null ? addon.modId : MeteorClient.MOD_ID;
                }
            }
        }
        return MeteorClient.MOD_ID;
    }

    public static void init() {
        // Meteor pseudo addon
        {
            MeteorClient.ADDON = new MeteorAddon() {
                @Override
                public void onInitialize() {}

                @Override
                public String getPackage() {
                    return "meteordevelopment.meteorclient";
                }

                @Override
                public String getWebsite() {
                    return "https://meteorclient.com";
                }

                @Override
                public GithubRepo getRepo() {
                    return new GithubRepo("MeteorDevelopment", "meteor-client");
                }

                @Override
                public String getCommit() {
                    String commit = MeteorClient.MOD_META.getCustomValue(MeteorClient.MOD_ID + ":commit").getAsString();
                    return commit.isEmpty() ? null : commit;
                }
            };

            ModMetadata metadata = FabricLoader.getInstance().getModContainer(MeteorClient.MOD_ID).get().getMetadata();

            MeteorClient.ADDON.name = metadata.getName();
            MeteorClient.ADDON.modId = MeteorClient.MOD_ID;
            MeteorClient.ADDON.authors = new String[metadata.getAuthors().size()];
            if (metadata.containsCustomValue(MeteorClient.MOD_ID + ":color")) {
                MeteorClient.ADDON.color.parse(metadata.getCustomValue(MeteorClient.MOD_ID + ":color").getAsString());
            }

            int i = 0;
            for (Person author : metadata.getAuthors()) {
                MeteorClient.ADDON.authors[i++] = author.getName();
            }

            ADDONS.add(MeteorClient.ADDON);
        }

        // Addons
        for (EntrypointContainer<MeteorAddon> entrypoint : FabricLoader.getInstance().getEntrypointContainers("meteor", MeteorAddon.class)) {
            ModMetadata metadata = entrypoint.getProvider().getMetadata();
            MeteorAddon addon;
            try {
                addon = entrypoint.getEntrypoint();
            } catch (Throwable throwable) {
                throw new RuntimeException("Exception during addon init \"%s\".".formatted(metadata.getName()), throwable);
            }

            addon.name = metadata.getName();
            addon.modId = metadata.getId();
            TranslationManager.registerNamespace(addon.modId);

            if (metadata.getAuthors().isEmpty()) throw new RuntimeException("Addon \"%s\" requires at least 1 author to be defined in it's fabric.mod.json. See https://fabricmc.net/wiki/documentation:fabric_mod_json_spec".formatted(addon.name));
            addon.authors = new String[metadata.getAuthors().size()];

            if (metadata.containsCustomValue(MeteorClient.MOD_ID + ":color")) {
                addon.color.parse(metadata.getCustomValue(MeteorClient.MOD_ID + ":color").getAsString());
            }

            int i = 0;
            for (Person author : metadata.getAuthors()) {
                addon.authors[i++] = author.getName();
            }

            ADDONS.add(addon);
        }
    }
}
