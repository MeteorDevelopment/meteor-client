/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules;

import minegame159.meteorclient.gui.GuiConfig;

public class Category {
    public final String name;
    private final int nameHash;

    public GuiConfig.WindowConfig windowConfig = new GuiConfig.WindowConfig();

    public Category(String name) {
        this.name = name;
        this.nameHash = name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return nameHash == category.nameHash;
    }

    @Override
    public int hashCode() {
        return nameHash;
    }
}
