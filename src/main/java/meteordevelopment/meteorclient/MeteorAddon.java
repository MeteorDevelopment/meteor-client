/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient;

import meteordevelopment.meteorclient.utils.render.color.Color;

import java.util.List;

public abstract class MeteorAddon {
    public final String name;
    public final List<String> authors;
    public final Color color;

    public MeteorAddon(String name, List<String> authors, Color color) {
        this.name = name;
        this.authors = authors;
        this.color = color;
    }

    public abstract void onInitialize();

    public void onRegisterCategories() {}
}
