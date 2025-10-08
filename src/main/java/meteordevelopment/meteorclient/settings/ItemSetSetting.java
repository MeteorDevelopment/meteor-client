/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import meteordevelopment.meteorclient.settings.groups.GroupSet;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ItemSetSetting extends GroupedSetSetting<Item> {

    public static final Groups<Item> GROUPS = new Groups<>();

    // TODO: resolve this (used in AutoSmelter)
    private final boolean bypassFilterWhenSavingAndLoading;

    public ItemSetSetting(String name, String description, GroupSet<Item, Groups<Item>.Group> defaultValue, Consumer<GroupSet<Item, Groups<Item>.Group>> onChanged, Consumer<Setting<GroupSet<Item, Groups<Item>.Group>>> onModuleActivated, IVisible visible, Predicate<Item> filter, boolean bypassFilterWhenSavingAndLoading) {
        super(name, description, defaultValue, filter, onChanged, onModuleActivated, visible);

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
    protected Groups<Item> groups() {
        return GROUPS;
    }

    @Override
    protected Registry<Item> suggestRegistry() {
        return Registries.ITEM;
    }

    public static class Builder extends SettingBuilder<Builder, GroupSet<Item, Groups<Item>.Group>, ItemSetSetting> {
        private Predicate<Item> filter = null;
        private boolean bypass = false;

        public Builder() {
            super(new GroupSet<>());
        }

        public Builder defaultValue(Collection<Item> defaults) {
            if (defaultValue == null)
                return defaultValue(defaults != null ? new GroupSet<>(defaults) : new GroupSet<>());
            defaultValue.addAll(defaults);
            return this;
        }

        public Builder defaultValue(Item... defaults) {
            if (defaultValue == null)
                return defaultValue(defaults != null ? new GroupSet<>(Arrays.asList(defaults)) : new GroupSet<>());
            defaultValue.addAll(Arrays.asList(defaults));
            return this;
        }

         @SafeVarargs
         public final Builder defaultGroups(Groups<Item>.Group... defaults) {
            List<Groups<Item>.Group> groups = null;

            if (defaults != null)
                groups = Arrays.stream(defaults).filter(g -> g.isOf(GROUPS)).toList();

            if (defaultValue == null)
                return defaultValue(groups != null ? new GroupSet<>(null, groups) : new GroupSet<>());

            if (groups != null) defaultValue.addAllGroups(groups);
            return this;
        }

        public Builder filter(Predicate<Item> filter) {
            this.filter = filter;
            return this;
        }

        @Override
        public ItemSetSetting build() {
            return new ItemSetSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, filter, bypass);
        }
    }

    // these are just for UI testing, they are not necessarily good ones to have
    public static Groups<Item>.Group FOOD, AXES, PICKAXES, SWORDS, HOES, TOOLS, TOOLS_STRICT, HELMETS, CHESTPLATES, LEGGINGS, BOOTS, ARMOR;
    static {

        FOOD = GROUPS.builtin("food-all", Items.APPLE)
            .items(Registries.ITEM.stream().filter(i -> i.getComponents().get(DataComponentTypes.FOOD) != null).toList())
            .get();

        TOOLS_STRICT = GROUPS.builtin("tools-all", Items.GOLDEN_AXE)
            .items(Registries.ITEM.stream().filter(i -> i.getComponents().get(DataComponentTypes.TOOL) != null).toList())
            .get();

        PICKAXES = GROUPS.builtin("picks", Items.IRON_PICKAXE)
            .items(Items.WOODEN_HOE, Items.STONE_HOE, Items.IRON_HOE, Items.GOLDEN_HOE, Items.DIAMOND_HOE, Items.NETHERITE_HOE)
            .get();

        AXES = GROUPS.builtin("axes", Items.IRON_AXE)
            .items(Items.WOODEN_HOE, Items.STONE_HOE, Items.IRON_HOE, Items.GOLDEN_HOE, Items.DIAMOND_HOE, Items.NETHERITE_HOE)
            .get();

        SWORDS = GROUPS.builtin("swords", Items.IRON_SWORD)
            .items(Items.WOODEN_HOE, Items.STONE_HOE, Items.IRON_HOE, Items.GOLDEN_HOE, Items.DIAMOND_HOE, Items.NETHERITE_HOE)
            .get();

        HOES = GROUPS.builtin("hoes", Items.IRON_HOE)
            .items(Items.WOODEN_HOE, Items.STONE_HOE, Items.IRON_HOE, Items.GOLDEN_HOE, Items.DIAMOND_HOE, Items.NETHERITE_HOE)
            .get();

        TOOLS = GROUPS.builtin("tools", Items.DIAMOND_AXE)
            .items(Items.SHEARS, Items.FLINT_AND_STEEL)
            .include(PICKAXES, AXES, HOES)
            .get();

        HELMETS = GROUPS.builtin("hoes", Items.IRON_HELMET)
            .items(Items.TURTLE_HELMET, Items.CHAINMAIL_HELMET, Items.IRON_HELMET, Items.GOLDEN_HELMET, Items.DIAMOND_HELMET, Items.NETHERITE_HELMET)
            .get();

        CHESTPLATES = GROUPS.builtin("chestplates", Items.IRON_CHESTPLATE)
            .items(Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE)
            .get();

        LEGGINGS = GROUPS.builtin("leggings", Items.IRON_LEGGINGS)
            .items(Items.CHAINMAIL_LEGGINGS, Items.IRON_LEGGINGS, Items.GOLDEN_LEGGINGS, Items.DIAMOND_LEGGINGS, Items.NETHERITE_LEGGINGS)
            .get();

        BOOTS = GROUPS.builtin("boots", Items.IRON_BOOTS)
            .items(Items.CHAINMAIL_BOOTS, Items.IRON_BOOTS, Items.GOLDEN_BOOTS, Items.DIAMOND_BOOTS, Items.NETHERITE_BOOTS)
            .get();

        ARMOR = GROUPS.builtin("armor", Items.DIAMOND_CHESTPLATE)
            .include(HELMETS, CHESTPLATES, LEGGINGS, BOOTS)
            .get();


    }
}
