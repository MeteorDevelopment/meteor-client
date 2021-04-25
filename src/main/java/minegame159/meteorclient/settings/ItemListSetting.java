/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.settings;

import net.minecraft.item.Item;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ItemListSetting extends Setting<List<Item>> {
    public final Predicate<Item> filter;

    public ItemListSetting(String name, String description, List<Item> defaultValue, Consumer<List<Item>> onChanged, Consumer<Setting<List<Item>>> onModuleActivated, Predicate<Item> filter) {
        super(name, description, defaultValue, onChanged, onModuleActivated);

        this.value = new ArrayList<>(defaultValue);
        this.filter = filter;
    }

    @Override
    protected List<Item> parseImpl(String str) {
        String[] values = str.split(",");
        List<Item> items = new ArrayList<>(values.length);

        try {
            for (String value : values) {
                Item item = parseId(Registry.ITEM, value);
                if (item != null && (filter == null || filter.test(item))) items.add(item);
            }
        } catch (Exception ignored) {}

        return items;
    }

    @Override
    public void reset(boolean callbacks) {
        value = new ArrayList<>(defaultValue);
        if (callbacks) changed();
    }

    @Override
    protected boolean isValueValid(List<Item> value) {
        return true;
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Registry.ITEM.getIds();
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = saveGeneral();

        ListTag valueTag = new ListTag();
        for (Item item : get()) {
            valueTag.add(StringTag.of(Registry.ITEM.getId(item).toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public List<Item> fromTag(CompoundTag tag) {
        get().clear();

        ListTag valueTag = tag.getList("value", 8);
        for (Tag tagI : valueTag) {
            Item item = Registry.ITEM.get(new Identifier(tagI.asString()));

            if (filter == null || filter.test(item)) get().add(item);
        }

        changed();
        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private List<Item> defaultValue;
        private Consumer<List<Item>> onChanged;
        private Consumer<Setting<List<Item>>> onModuleActivated;
        private Predicate<Item> filter;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultValue(List<Item> defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder onChanged(Consumer<List<Item>> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder onModuleActivated(Consumer<Setting<List<Item>>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public Builder filter(Predicate<Item> filter) {
            this.filter = filter;
            return this;
        }

        public ItemListSetting build() {
            return new ItemListSetting(name, description, defaultValue, onChanged, onModuleActivated, filter);
        }
    }
}
