/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import net.minecraft.block.Block;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

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
                Block block = parseId(Registries.BLOCK, value);
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
        return Registries.BLOCK.getIds();
    }

    @Override
    protected NbtCompound save(NbtCompound tag) {
        NbtList valueTag = new NbtList();
        for (Block block : get()) {
            valueTag.add(NbtString.of(Registries.BLOCK.getId(block).toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    protected List<Block> load(NbtCompound tag) {
        get().clear();

        NbtList valueTag = tag.getList("value", 8);
        for (NbtElement tagI : valueTag) {
            Block block = Registries.BLOCK.get(Identifier.of(tagI.asString()));

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
