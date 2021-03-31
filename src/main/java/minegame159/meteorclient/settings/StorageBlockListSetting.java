/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.settings;

import com.mojang.serialization.Lifecycle;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import org.apache.logging.log4j.core.util.ObjectArrayIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class StorageBlockListSetting extends Setting<List<BlockEntityType<?>>> {
    public static final BlockEntityType<?>[] STORAGE_BLOCKS = { BlockEntityType.FURNACE, BlockEntityType.CHEST, BlockEntityType.TRAPPED_CHEST, BlockEntityType.ENDER_CHEST, BlockEntityType.DISPENSER, BlockEntityType.DROPPER, BlockEntityType.HOPPER, BlockEntityType.SHULKER_BOX, BlockEntityType.BARREL, BlockEntityType.SMOKER, BlockEntityType.BLAST_FURNACE };

    public static final Registry<BlockEntityType<?>> REGISTRY = new SRegistry();

    public StorageBlockListSetting(String name, String description, List<BlockEntityType<?>> defaultValue, Consumer<List<BlockEntityType<?>>> onChanged, Consumer<Setting<List<BlockEntityType<?>>>> onModuleActivated) {
        super(name, description, defaultValue, onChanged, onModuleActivated);

        value = new ArrayList<>(defaultValue);
    }

    @Override
    public void reset(boolean callbacks) {
        value = new ArrayList<>(defaultValue);
        if (callbacks) changed();
    }

    @Override
    protected List<BlockEntityType<?>> parseImpl(String str) {
        String[] values = str.split(",");
        List<BlockEntityType<?>> blocks = new ArrayList<>(values.length);

        try {
            for (String value : values) {
                BlockEntityType<?> block = parseId(Registry.BLOCK_ENTITY_TYPE, value);
                if (block != null) blocks.add(block);
            }
        } catch (Exception ignored) {}

        return blocks;
    }

    @Override
    protected boolean isValueValid(List<BlockEntityType<?>> value) {
        return true;
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Registry.BLOCK_ENTITY_TYPE.getIds();
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = saveGeneral();

        ListTag valueTag = new ListTag();
        for (BlockEntityType<?> type : get()) {
            Identifier id = Registry.BLOCK_ENTITY_TYPE.getId(type);
            if (id != null) valueTag.add(StringTag.of(id.toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public List<BlockEntityType<?>> fromTag(CompoundTag tag) {
        get().clear();

        ListTag valueTag = tag.getList("value", 8);
        for (Tag tagI : valueTag) {
            BlockEntityType<?> type = Registry.BLOCK_ENTITY_TYPE.get(new Identifier(tagI.asString()));
            if (type != null) get().add(type);
        }

        changed();
        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private List<BlockEntityType<?>> defaultValue;
        private Consumer<List<BlockEntityType<?>>> onChanged;
        private Consumer<Setting<List<BlockEntityType<?>>>> onModuleActivated;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultValue(List<BlockEntityType<?>> defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder onChanged(Consumer<List<BlockEntityType<?>>> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder onModuleActivated(Consumer<Setting<List<BlockEntityType<?>>>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public StorageBlockListSetting build() {
            return new StorageBlockListSetting(name, description, defaultValue, onChanged, onModuleActivated);
        }
    }

    private static class SRegistry extends Registry<BlockEntityType<?>> {
        public SRegistry() {
            super(RegistryKey.ofRegistry(new Identifier("meteor-client", "storage-blocks")), Lifecycle.stable());
        }

        @Nullable
        @Override
        public Identifier getId(BlockEntityType<?> entry) {
            return null;
        }

        @Override
        public Optional<RegistryKey<BlockEntityType<?>>> getKey(BlockEntityType<?> entry) {
            return Optional.empty();
        }

        @Override
        public int getRawId(@Nullable BlockEntityType<?> entry) {
            return 0;
        }

        @Nullable
        @Override
        public BlockEntityType<?> get(@Nullable RegistryKey<BlockEntityType<?>> key) {
            return null;
        }

        @Nullable
        @Override
        public BlockEntityType<?> get(@Nullable Identifier id) {
            return null;
        }

        @Override
        protected Lifecycle getEntryLifecycle(BlockEntityType<?> object) {
            return null;
        }

        @Override
        public Lifecycle getLifecycle() {
            return null;
        }

        @Override
        public Set<Identifier> getIds() {
            return null;
        }

        @Override
        public Set<Map.Entry<RegistryKey<BlockEntityType<?>>, BlockEntityType<?>>> getEntries() {
            return null;
        }

        @Override
        public boolean containsId(Identifier id) {
            return false;
        }

        @Nullable
        @Override
        public BlockEntityType<?> get(int index) {
            return null;
        }

        @NotNull
        @Override
        public Iterator<BlockEntityType<?>> iterator() {
            return new ObjectArrayIterator<>(STORAGE_BLOCKS);
        }
    }
}
