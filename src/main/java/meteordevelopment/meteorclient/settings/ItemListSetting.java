/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import com.google.common.base.Predicates;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ItemListSetting extends AbstractRegistryListSetting<Set<Item>, Item> {
    private final boolean bypassFilterWhenSavingAndLoading;

    public ItemListSetting(String name, String description, Set<Item> defaultValue, Consumer<Set<Item>> onChanged, Consumer<Setting<Set<Item>>> onModuleActivated, IVisible visible, Predicate<Item> filter, boolean bypassFilterWhenSavingAndLoading) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible, filter, Registries.ITEM);
        this.bypassFilterWhenSavingAndLoading = bypassFilterWhenSavingAndLoading;
    }

    @Override
    protected Set<Item> transferCollection(Collection<Item> from) {
        return new ReferenceOpenHashSet<>(from);
    }

    @Override
    protected NbtCompound save(NbtCompound tag) {
        NbtList valueTag = new NbtList();
        for (Item item : get()) {
            if (bypassFilterWhenSavingAndLoading || (filter == null || filter.test(item))) valueTag.add(NbtString.of(Registries.ITEM.getId(item).toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public Set<Item> load(NbtCompound tag) {
        get().clear();

        NbtList valueTag = tag.getListOrEmpty("value");
        for (NbtElement tagI : valueTag) {
            Item item = Registries.ITEM.get(Identifier.of(tagI.asString().orElse("")));

            if (bypassFilterWhenSavingAndLoading || (filter == null || filter.test(item))) get().add(item);
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, Set<Item>, ItemListSetting> {
        private Predicate<Item> filter = Predicates.alwaysTrue();
        private boolean bypassFilterWhenSavingAndLoading;

        public Builder() {
            super(Collections.emptySet());
        }

        public Builder defaultValue(Item... defaults) {
            return defaultValue(defaults != null ? ReferenceOpenHashSet.of(defaults) : new ReferenceOpenHashSet<>());
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
