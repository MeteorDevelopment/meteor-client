/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.settings;

import net.minecraft.block.Block;
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

public class BlockListSetting extends Setting<List<Block>> {
    public final Predicate<Block> filter;

    public BlockListSetting(String name, String description, List<Block> defaultValue, Consumer<List<Block>> onChanged, Consumer<Setting<List<Block>>> onModuleActivated, Predicate<Block> filter) {
        super(name, description, defaultValue, onChanged, onModuleActivated);

        this.filter = filter;
        this.value = new ArrayList<>(defaultValue);
    }

    @Override
    public void reset(boolean callbacks) {
        value = new ArrayList<>(defaultValue);
        if (callbacks) changed();
    }

    @Override
    protected List<Block> parseImpl(String str) {
        String[] values = str.split(",");
        List<Block> blocks = new ArrayList<>(values.length);

        try {
            for (String value : values) {
                Block block = parseId(Registry.BLOCK, value);
                if (block != null && (filter == null || filter.test(block))) blocks.add(block);
            }
        } catch (Exception ignored) {}

        return blocks;
    }

    @Override
    protected boolean isValueValid(List<Block> value) {
        return true;
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Registry.BLOCK.getIds();
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = saveGeneral();

        ListTag valueTag = new ListTag();
        for (Block block : get()) {
            valueTag.add(StringTag.of(Registry.BLOCK.getId(block).toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public List<Block> fromTag(CompoundTag tag) {
        get().clear();

        ListTag valueTag = tag.getList("value", 8);
        for (Tag tagI : valueTag) {
            Block block = Registry.BLOCK.get(new Identifier(tagI.asString()));

            if (filter == null || filter.test(block)) get().add(block);
        }

        changed();
        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private List<Block> defaultValue;
        private Consumer<List<Block>> onChanged;
        private Consumer<Setting<List<Block>>> onModuleActivated;
        private Predicate<Block> filter;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultValue(List<Block> defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder onChanged(Consumer<List<Block>> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder onModuleActivated(Consumer<Setting<List<Block>>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public Builder filter(Predicate<Block> filter) {
            this.filter = filter;
            return this;
        }

        public BlockListSetting build() {
            return new BlockListSetting(name, description, defaultValue, onChanged, onModuleActivated, filter);
        }
    }
}
