/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class ItemSetting extends AbstractRegistryItemSetting<Item> {
    public ItemSetting(String name, String description, Item defaultValue, Consumer<Item> onChanged, Consumer<Setting<Item>> onModuleActivated, IVisible visible, Predicate<Item> filter) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible, filter, Registries.ITEM);
    }

    public static class Builder extends AbstractBuilder<Builder, Item, ItemSetting> {
        @Override
        public ItemSetting build() {
            return new ItemSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, filter);
        }
    }
}
