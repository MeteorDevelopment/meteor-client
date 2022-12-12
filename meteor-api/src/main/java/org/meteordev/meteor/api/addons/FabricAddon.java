/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package org.meteordev.meteor.api.addons;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;

public abstract class FabricAddon implements Addon {
    protected final ModMetadata metadata;

    private String[] authors;

    public FabricAddon(String id) {
        ModContainer container = FabricLoader.getInstance().getModContainer(id)
            .orElseThrow(() -> new IllegalStateException("Mod with id '" + id + "' is not loaded."));

        metadata = container.getMetadata();
    }

    @Override
    public String getId() {
        return metadata.getId();
    }

    @Override
    public String getName() {
        return metadata.getName();
    }

    @Override
    public String[] getAuthors() {
        if (authors == null) {
            authors = new String[metadata.getAuthors().size()];

            int i = 0;
            for (Person person : metadata.getAuthors()) {
                authors[i++] = person.getName();
            }
        }

        return authors;
    }
}
