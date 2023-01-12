/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules;

import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class Category {
    public final String name;
    public final ItemStack icon;
    public final Color color;
    private final int nameHash;

    public Category(String name, ItemStack icon, Color color) {
        this.name = name;
        this.nameHash = name.hashCode();
        this.icon = icon == null ? Items.AIR.getDefaultStack() : icon;
        this.color = color == null ? Color.fromHsv(Utils.random(0.0, 360.0), 0.35, 1) : color;
    }

    public Category(String name, ItemStack icon) {
        this(name, icon, null);
    }

    public Category(String name) {
        this(name, null, null);
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
