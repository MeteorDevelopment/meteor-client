/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import meteordevelopment.meteorclient.settings.groups.GroupedList;
import meteordevelopment.meteorclient.settings.groups.ListGroupTracker;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BlockListSetting extends GroupedListSetting<Block> {

    private final static Map<String, Group> GROUPS = new HashMap<>();

    public BlockListSetting(String name, String description, GroupedList<Block, Group> defaultValue, Consumer<GroupedList<Block, Group>> onChanged, Consumer<Setting<GroupedList<Block, Group>>> onModuleActivated, Predicate<Block> filter, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        if (GROUPS.isEmpty()) initGroups();

        this.filter = filter;
    }

    @Override
    public Block parseItem(String str) {
        Block block = parseId(Registries.BLOCK, str);
        if (block != null && (filter == null || filter.test(block))) return block;
        return null;
    }

    @Override
    public NbtElement itemToNbt(Block block) {
        return NbtString.of(Registries.BLOCK.getId(block).toString());
    }

    @Override
    public Block itemFromNbt(NbtElement e) {
        Block block = Registries.BLOCK.get(Identifier.of(e.asString().orElse("")));
        if (filter == null || filter.test(block)) return block;
        return null;
    }

    @Override
    public Map<String, GroupedListSetting<Block>.Group> groups() {
        return GROUPS;
    }

    private final static ListGroupTracker tracker = new ListGroupTracker();

    @Override
    protected ListGroupTracker tracker() {
        return tracker;
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Registries.BLOCK.getIds();
    }

    public static class Builder extends SettingBuilder<Builder, GroupedList<Block, Group>, BlockListSetting> {
        private Predicate<Block> filter;

        public Builder() {
            super(new GroupedList<>());
        }

        public Builder defaultValue(Collection<Block> defaults) {
            if (defaultValue == null)
                return defaultValue(defaults != null ? new GroupedList<>(defaults) : new GroupedList<>());
            defaultValue.addAll(defaults);
            return this;
        }

        public Builder defaultValue(Block... defaults) {
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

        public Builder filter(Predicate<Block> filter) {
            this.filter = filter;
            return this;
        }

        @Override
        public BlockListSetting build() {
            return new BlockListSetting(name, description, defaultValue, onChanged, onModuleActivated, filter, visible);
        }
    }

    public static Group ORES, DIRTS, SANDS, STONES, STONES_ALL, TERRAIN, TERRAIN_ALL;

    private void initGroups() {
        ORES = builtin("Ores", Items.DIAMOND_ORE)
            .items(Xray.ORES).get();
        DIRTS = builtin("Dirt", Items.DIRT)
            .items(Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.DIRT_PATH, Blocks.PODZOL, Blocks.COARSE_DIRT, Blocks.CLAY)
            .get();
        SANDS = builtin("Sand", Items.SAND)
            .items(Blocks.SAND, Blocks.RED_SAND, Blocks.GRAVEL)
            .get();
        STONES = builtin("Stone", Items.STONE)
            .items(Blocks.STONE, Blocks.DEEPSLATE, Blocks.NETHERRACK, Blocks.SANDSTONE, Blocks.TUFF, Blocks.BASALT)
            .get();
        STONES_ALL = builtin("Stone-all", Items.DIORITE)
            .items(Blocks.ANDESITE, Blocks.DIORITE, Blocks.GRANITE, Blocks.BLACKSTONE, Blocks.CALCITE)
            .include(STONES)
            .get();
        TERRAIN = builtin("Terrain", Items.GRASS_BLOCK)
            .include(STONES, DIRTS, SANDS)
            .get();
        TERRAIN_ALL = builtin("Terrain-all", Items.GRASS_BLOCK)
            .include(STONES_ALL, DIRTS, SANDS)
            .get();
    }
}
