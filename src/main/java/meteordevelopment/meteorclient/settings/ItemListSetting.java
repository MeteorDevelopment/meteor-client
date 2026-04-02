/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ItemListSetting extends Setting<List<Item>> {
    public final Predicate<Item> filter;
    private final boolean bypassFilterWhenSavingAndLoading;

    public ItemListSetting(String name, String description, List<Item> defaultValue, Consumer<List<Item>> onChanged, Consumer<Setting<List<Item>>> onModuleActivated, IVisible visible, Predicate<Item> filter, boolean bypassFilterWhenSavingAndLoading) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        this.filter = filter;
        this.bypassFilterWhenSavingAndLoading = bypassFilterWhenSavingAndLoading;
    }

    @Override
    protected List<Item> parseImpl(String str) {
        String[] values = str.split(",");
        List<Item> items = new ArrayList<>(values.length);

        try {
            for (String value : values) {
                Item item = parseId(BuiltInRegistries.ITEM, value);
                if (item != null && (filter == null || filter.test(item))) items.add(item);
            }
        } catch (Exception ignored) {
        }

        return items;
    }

    @Override
    public void resetImpl() {
        value = new ArrayList<>(defaultValue);
    }

    @Override
    protected boolean isValueValid(List<Item> value) {
        return true;
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return BuiltInRegistries.ITEM.keySet();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag valueTag = new ListTag();
        for (Item item : get()) {
            if (bypassFilterWhenSavingAndLoading || (filter == null || filter.test(item)))
                valueTag.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(item).toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public List<Item> load(CompoundTag tag) {
        get().clear();

        ListTag valueTag = tag.getListOrEmpty("value");
        for (Tag tagI : valueTag) {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(tagI.asString().orElse("")));

            if (bypassFilterWhenSavingAndLoading || (filter == null || filter.test(item))) get().add(item);
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, List<Item>, ItemListSetting> {
        private Predicate<Item> filter;
        private boolean bypassFilterWhenSavingAndLoading;

        public Builder() {
            super(new ArrayList<>(0));
        }

        public Builder defaultValue(Item... defaults) {
            return defaultValue(defaults != null ? Arrays.asList(defaults) : new ArrayList<>());
        }

        public Builder filter(Predicate<Item> filter) {
            this.filter = filter;
            return this;
        }

        public Builder bypassFilterWhenSavingAndLoading() {
            this.bypassFilterWhenSavingAndLoading = true;
            return this;
        }

        @Override
        public ItemListSetting build() {
            return new ItemListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, filter, bypassFilterWhenSavingAndLoading);
        }
    }
}
