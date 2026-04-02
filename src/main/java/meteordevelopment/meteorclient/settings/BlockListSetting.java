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
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BlockListSetting extends Setting<List<Block>> {
    public final Predicate<Block> filter;

    public BlockListSetting(String name, String description, List<Block> defaultValue, Consumer<List<Block>> onChanged, Consumer<Setting<List<Block>>> onModuleActivated, Predicate<Block> filter, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        this.filter = filter;
    }

    @Override
    public void resetImpl() {
        value = new ArrayList<>(defaultValue);
    }

    @Override
    protected List<Block> parseImpl(String str) {
        String[] values = str.split(",");
        List<Block> blocks = new ArrayList<>(values.length);

        try {
            for (String value : values) {
                Block block = parseId(BuiltInRegistries.BLOCK, value);
                if (block != null && (filter == null || filter.test(block))) blocks.add(block);
            }
        } catch (Exception ignored) {
        }

        return blocks;
    }

    @Override
    protected boolean isValueValid(List<Block> value) {
        return true;
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return BuiltInRegistries.BLOCK.keySet();
    }

    @Override
    protected CompoundTag save(CompoundTag tag) {
        ListTag valueTag = new ListTag();
        for (Block block : get()) {
            valueTag.add(StringTag.valueOf(BuiltInRegistries.BLOCK.getKey(block).toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    protected List<Block> load(CompoundTag tag) {
        get().clear();

        ListTag valueTag = tag.getListOrEmpty("value");
        for (Tag tagI : valueTag) {
            Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(tagI.asString().orElse("")));

            if (filter == null || filter.test(block)) get().add(block);
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, List<Block>, BlockListSetting> {
        private Predicate<Block> filter;

        public Builder() {
            super(new ArrayList<>(0));
        }

        public Builder defaultValue(Block... defaults) {
            return defaultValue(defaults != null ? Arrays.asList(defaults) : new ArrayList<>());
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
}
