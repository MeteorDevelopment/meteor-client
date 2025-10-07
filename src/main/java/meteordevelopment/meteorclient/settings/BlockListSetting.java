/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import meteordevelopment.meteorclient.settings.groups.GroupedList;
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

    public BlockListSetting(String name, String description, GroupedList<Block, Groups<Block>.Group> defaultValue, Consumer<GroupedList<Block, Groups<Block>.Group>> onChanged, Consumer<Setting<GroupedList<Block, Groups<Block>.Group>>> onModuleActivated, Predicate<Block> filter, IVisible visible) {
        super(name, description, defaultValue, filter, onChanged, onModuleActivated, visible);
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

    public static final Groups<Block> GROUPS = new Groups<>();

    @Override
    protected Groups<Block> groups() {
        return GROUPS;
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Registries.BLOCK.getIds();
    }

    public static class Builder extends SettingBuilder<Builder, GroupedList<Block, Groups<Block>.Group>, BlockListSetting> {
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
         public final Builder defaultGroups(Groups<Block>.Group... defaults) {
            List<Groups<Block>.Group> groups = null;

            if (defaults != null)
                groups = Arrays.stream(defaults).filter(g -> g.trackerIs(GROUPS)).toList();

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

    public static Groups<Block>.Group ORES, DIRTS, SANDS, STONES, STONES_ALL, TERRAIN, TERRAIN_ALL;

    static {
        ORES = GROUPS.builtin("ores", Items.DIAMOND_ORE)
            .items(Xray.ORES).get();
        DIRTS = GROUPS.builtin("dirt", Items.DIRT)
            .items(Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.DIRT_PATH, Blocks.PODZOL, Blocks.COARSE_DIRT, Blocks.CLAY)
            .get();
        SANDS = GROUPS.builtin("sand", Items.SAND)
            .items(Blocks.SAND, Blocks.RED_SAND, Blocks.GRAVEL)
            .get();
        STONES = GROUPS.builtin("stone", Items.STONE)
            .items(Blocks.STONE, Blocks.DEEPSLATE, Blocks.NETHERRACK, Blocks.SANDSTONE, Blocks.TUFF, Blocks.BASALT)
            .get();
        STONES_ALL = GROUPS.builtin("stone-all", Items.DIORITE)
            .items(Blocks.ANDESITE, Blocks.DIORITE, Blocks.GRANITE, Blocks.BLACKSTONE, Blocks.CALCITE)
            .include(STONES)
            .get();
        TERRAIN = GROUPS.builtin("terrain", Items.GRASS_BLOCK)
            .include(STONES, DIRTS, SANDS)
            .get();
        TERRAIN_ALL = GROUPS.builtin("terrain-all", Items.GRASS_BLOCK)
            .include(STONES_ALL, DIRTS, SANDS)
            .get();
    }
}
