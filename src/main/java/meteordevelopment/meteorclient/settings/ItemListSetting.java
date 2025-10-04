/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.groups.GroupedList;
import meteordevelopment.meteorclient.settings.groups.ListGroupTracker;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ItemListSetting extends GroupedListSetting<Item> {

    private final static Map<String, Group> GROUPS = new HashMap<>();
    private final static ListGroupTracker tracker = new ListGroupTracker();

    private final boolean bypassFilterWhenSavingAndLoading;

    public ItemListSetting(String name, String description, GroupedList<Item, GroupedListSetting<Item>.Group> defaultValue, Consumer<GroupedList<Item, GroupedListSetting<Item>.Group>> onChanged, Consumer<Setting<GroupedList<Item, GroupedListSetting<Item>.Group>>> onModuleActivated, IVisible visible, Predicate<Item> filter, boolean bypassFilterWhenSavingAndLoading) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        if (GROUPS.isEmpty()) initGroups();

        this.filter = filter;
        this.bypassFilterWhenSavingAndLoading = bypassFilterWhenSavingAndLoading;
    }

    @Override
    public Item parseItem(String str) {
        Item item = parseId(Registries.ITEM, str);
        if (item != null && (filter == null || filter.test(item))) return item;
        return null;
    }

    @Override
    public NbtElement itemToNbt(Item item) {
        return NbtString.of(Registries.ITEM.getId(item).toString());
    }

    @Override
    public Item itemFromNbt(NbtElement e) {
        return Registries.ITEM.get(Identifier.of(e.asString().orElse("")));
    }

    @Override
    public Map<String, GroupedListSetting<Item>.Group> groups() {
        return GROUPS;
    }

    @Override
    protected ListGroupTracker tracker() {
        return tracker;
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Registries.ITEM.getIds();
    }

   public static class Builder extends SettingBuilder<Builder, GroupedList<Item, Group>, ItemListSetting> {
        private Predicate<Item> filter = null;
        private boolean bypass = false;

        public Builder() {
            super(new GroupedList<>());
        }

        public Builder defaultValue(Collection<Item> defaults) {
            if (defaultValue == null)
                return defaultValue(defaults != null ? new GroupedList<>(defaults) : new GroupedList<>());
            defaultValue.addAll(defaults);
            return this;
        }

        public Builder defaultValue(Item... defaults) {
            if (defaultValue == null)
                return defaultValue(defaults != null ? new GroupedList<>(Arrays.asList(defaults)) : new GroupedList<>());
            defaultValue.addAll(Arrays.asList(defaults));
            return this;
        }

         @SafeVarargs
         public final Builder defaultGroups(Group... defaults) {
            List<Group> groups = null;

            if (defaults != null)
                groups = Arrays.stream(defaults).filter(g -> g.trackerIs(tracker)).toList();

            if (defaultValue == null)
                return defaultValue(groups != null ? new GroupedList<>(null, groups) : new GroupedList<>());

            if (groups != null) defaultValue.addAllGroups(groups);
            return this;
        }

        public Builder filter(Predicate<Item> filter) {
            this.filter = filter;
            return this;
        }

        @Override
        public ItemListSetting build() {
            return new ItemListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, filter, bypass);
        }
    }
    public static Group TEST;

    private void initGroups() {

        MeteorClient.LOG.info("initGroups@ creating TEST");

        TEST = builtin("TEST", Items.DIAMOND_AXE)
            .items(Items.DIAMOND_AXE, Items.DRAGON_EGG).get();

        MeteorClient.LOG.info("initGroups@ created TEST with {} items", TEST.get().size());

        TEST.add(Items.RED_BUNDLE);

        MeteorClient.LOG.info("initGroups@ added item to TEST, now {} items", TEST.get().size());
    }
}
