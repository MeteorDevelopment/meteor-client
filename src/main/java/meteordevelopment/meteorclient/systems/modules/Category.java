/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules;

import meteordevelopment.meteorclient.utils.misc.MeteorTranslations;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class Category {
    public final String name;
    public final String translationKey;
    public final ItemStack icon;
    private final int nameHash;

    public Category(String name, ItemStack icon) {
        this.name = name;
        this.translationKey = "category." + name;
        this.nameHash = name.hashCode();
        this.icon = icon == null ? Items.AIR.getDefaultStack() : icon;
    }
    public Category(String name) {
        this(name, null);
    }

    public String getName() {
        return MeteorTranslations.translate(this.translationKey);
    }

    @Override
    public String toString() {
        return this.getName();
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
