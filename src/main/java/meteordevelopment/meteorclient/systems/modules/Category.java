/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class Category {
    public final String name;
    private ItemStack icon;
    private Item iconItem;
    private final int nameHash;

    public Category(String name, ItemStack icon) {
        this.name = name;
        this.nameHash = name.hashCode();
        setIcon(icon);
    }

    public Category(String name, Item icon) {
        this.name = name;
        this.nameHash = name.hashCode();
        setIcon(icon);
    }

    public Category(String name) {
        this(name, (Item) null);
    }

    public void setIcon(ItemStack icon) {
        this.icon = normalizeIcon(icon);
        this.iconItem = null;
    }

    public void setIcon(Item icon) {
        this.icon = ItemStack.EMPTY;
        this.iconItem = icon == null ? Items.AIR : icon;
    }

    public ItemStack getIcon() {
        if (!icon.isEmpty()) return icon.copy();
        return iconItem != null ? iconItem.getDefaultInstance() : ItemStack.EMPTY;
    }

    private static ItemStack normalizeIcon(ItemStack icon) {
        return icon == null ? ItemStack.EMPTY : icon.copy();
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
