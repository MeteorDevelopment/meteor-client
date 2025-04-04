/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.addons;

import motordevelopment.motorclient.MotorClient;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;

import java.util.ArrayList;
import java.util.List;

public class AddonManager {
    public static final List<MotorAddon> ADDONS = new ArrayList<>();

    public static void init() {
        // motor pseudo addon
        {
            MotorClient.ADDON = new MotorAddon() {
                @Override
                public void onInitialize() {}

                @Override
                public String getPackage() {
                    return "motordevelopment.motorclient";
                }

                @Override
                public String getWebsite() {
                    return "https://motorclient.com";
                }

                @Override
                public GithubRepo getRepo() {
                    return new GithubRepo("motorDevelopment", "motor-client");
                }

                @Override
                public String getCommit() {
                    String commit = MotorClient.MOD_META.getCustomValue(MotorClient.MOD_ID + ":commit").getAsString();
                    return commit.isEmpty() ? null : commit;
                }
            };

            ModMetadata metadata = FabricLoader.getInstance().getModContainer(MotorClient.MOD_ID).get().getMetadata();

            MotorClient.ADDON.name = metadata.getName();
            MotorClient.ADDON.authors = new String[metadata.getAuthors().size()];
            if (metadata.containsCustomValue(MotorClient.MOD_ID + ":color")) {
                MotorClient.ADDON.color.parse(metadata.getCustomValue(MotorClient.MOD_ID + ":color").getAsString());
            }

            int i = 0;
            for (Person author : metadata.getAuthors()) {
                MotorClient.ADDON.authors[i++] = author.getName();
            }

            ADDONS.add(MotorClient.ADDON);
        }

        // Addons
        for (EntrypointContainer<MotorAddon> entrypoint : FabricLoader.getInstance().getEntrypointContainers("motor", MotorAddon.class)) {
            ModMetadata metadata = entrypoint.getProvider().getMetadata();
            MotorAddon addon;
            try {
                addon = entrypoint.getEntrypoint();
            } catch (Throwable throwable) {
                throw new RuntimeException("Exception during addon init \"%s\".".formatted(metadata.getName()), throwable);
            }

            addon.name = metadata.getName();

            if (metadata.getAuthors().isEmpty()) throw new RuntimeException("Addon \"%s\" requires at least 1 author to be defined in it's fabric.mod.json. See https://fabricmc.net/wiki/documentation:fabric_mod_json_spec".formatted(addon.name));
            addon.authors = new String[metadata.getAuthors().size()];

            if (metadata.containsCustomValue(MotorClient.MOD_ID + ":color")) {
                addon.color.parse(metadata.getCustomValue(MotorClient.MOD_ID + ":color").getAsString());
            }

            int i = 0;
            for (Person author : metadata.getAuthors()) {
                addon.authors[i++] = author.getName();
            }

            ADDONS.add(addon);
        }
    }
}
