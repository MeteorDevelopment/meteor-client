/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules;

import meteordevelopment.meteorclient.translation.TranslationKey;
import meteordevelopment.meteorclient.translation.TranslationManager;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class Category {
    public final String name;
    public final Supplier<ItemStack> icon;
    private final int nameHash;
    private final TranslationKey nameKey;

    public Category(String name, Supplier<ItemStack> icon) {
        this.name = name;
        this.nameHash = name.hashCode();
        this.nameKey = TranslationKey.of(TranslationManager.categoryNameKey(name));
        this.icon = icon == null ? () -> ItemStack.EMPTY : icon;
    }

    public Category(String name) {
        this(name, null);
    }

    /** The category display title in the current language, resolved lazily. */
    public String displayTitle() {
        return nameKey.get();
    }

    public TranslationKey nameKey() {
        return nameKey;
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
