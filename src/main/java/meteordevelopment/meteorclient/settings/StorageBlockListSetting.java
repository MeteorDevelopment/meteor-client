/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.ObjectIterators;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class StorageBlockListSetting extends Setting<List<BlockEntityType<?>>> {
    public static final BlockEntityType<?>[] STORAGE_BLOCKS = new BlockEntityType[]{
        BlockEntityType.BARREL,
        BlockEntityType.BLAST_FURNACE,
        BlockEntityType.BREWING_STAND,
        BlockEntityType.CAMPFIRE,
        BlockEntityType.CHEST,
        BlockEntityType.CHISELED_BOOKSHELF,
        BlockEntityType.CRAFTER,
        BlockEntityType.DISPENSER,
        BlockEntityType.DECORATED_POT,
        BlockEntityType.DROPPER,
        BlockEntityType.ENDER_CHEST,
        BlockEntityType.FURNACE,
        BlockEntityType.HOPPER,
        BlockEntityType.SHULKER_BOX,
        BlockEntityType.SMOKER,
        BlockEntityType.TRAPPED_CHEST,
    };

    public static final Registry<BlockEntityType<?>> REGISTRY = new SRegistry();

    public StorageBlockListSetting(String name, String description, List<BlockEntityType<?>> defaultValue, Consumer<List<BlockEntityType<?>>> onChanged, Consumer<Setting<List<BlockEntityType<?>>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    public void resetImpl() {
        value = new ArrayList<>(defaultValue);
    }

    @Override
    protected List<BlockEntityType<?>> parseImpl(String str) {
        String[] values = str.split(",");
        List<BlockEntityType<?>> blocks = new ArrayList<>(values.length);

        try {
            for (String value : values) {
                BlockEntityType<?> block = parseId(BuiltInRegistries.BLOCK_ENTITY_TYPE, value);
                if (block != null) blocks.add(block);
            }
        } catch (Exception ignored) {
        }

        return blocks;
    }

    @Override
    protected boolean isValueValid(List<BlockEntityType<?>> value) {
        return true;
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return BuiltInRegistries.BLOCK_ENTITY_TYPE.getIds();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag valueTag = new NbtList();
        for (BlockEntityType<?> type : get()) {
            Identifier id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getId(type);
            if (id != null) valueTag.add(StringTag.of(id.toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public List<BlockEntityType<?>> load(CompoundTag tag) {
        get().clear();

        ListTag valueTag = tag.getListOrEmpty("value");
        for (Tag tagI : valueTag) {
            BlockEntityType<?> type = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(Identifier.of(tagI.asString().orElse("")));
            if (type != null) get().add(type);
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, List<BlockEntityType<?>>, StorageBlockListSetting> {
        public Builder() {
            super(new ArrayList<>(0));
        }

        public Builder defaultValue(BlockEntityType<?>... defaults) {
            return defaultValue(defaults != null ? Arrays.asList(defaults) : new ArrayList<>());
        }

        @Override
        public StorageBlockListSetting build() {
            return new StorageBlockListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }

    private static class SRegistry extends MappedRegistry<BlockEntityType<?>> {
        public SRegistry() {
            super(ResourceKey.ofRegistry(MeteorClient.identifier("storage-blocks")), Lifecycle.stable());
        }

        @Override
        public int size() {
            return STORAGE_BLOCKS.length;
        }

        @Nullable
        @Override
        public Identifier getId(BlockEntityType<?> entry) {
            return null;
        }

        @Override
        public Optional<ResourceKey<BlockEntityType<?>>> getKey(BlockEntityType<?> entry) {
            return Optional.empty();
        }

        @Override
        public int getRawId(@Nullable BlockEntityType<?> entry) {
            return 0;
        }

        @Nullable
        @Override
        public BlockEntityType<?> get(@Nullable ResourceKey<BlockEntityType<?>> key) {
            return null;
        }

        @Nullable
        @Override
        public BlockEntityType<?> get(@Nullable Identifier id) {
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
        public BlockEntityType<?> getOrThrow(int index) {
            return super.getOrThrow(index);
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
            return ObjectIterators.wrap(STORAGE_BLOCKS);
        }

        @Override
        public boolean contains(ResourceKey<BlockEntityType<?>> key) {
            return false;
        }

        @Override
        public Set<Map.Entry<ResourceKey<BlockEntityType<?>>, BlockEntityType<?>>> getEntrySet() {
            return null;
        }

        @Override
        public Optional<Holder.Reference<BlockEntityType<?>>> getRandom(net.minecraft.util.math.random.Random random) {
            return Optional.empty();
        }

        @Override
        public Registry<BlockEntityType<?>> freeze() {
            return null;
        }

        @Override
        public Holder.Reference<BlockEntityType<?>> createEntry(BlockEntityType<?> value) {
            return null;
        }

        @Override
        public Optional<Holder.Reference<BlockEntityType<?>>> getEntry(int rawId) {
            return Optional.empty();
        }

        @Override
        public Optional<Holder.Reference<BlockEntityType<?>>> getEntry(Identifier id) {
            return Optional.empty();
        }

        @Override
        public Stream<Holder.Reference<BlockEntityType<?>>> streamEntries() {
            return null;
        }

        @Override
        public Stream<HolderSet.Named<BlockEntityType<?>>> streamTags() {
            return null;
        }

        @Override
        public Set<ResourceKey<BlockEntityType<?>>> getKeys() {
            return null;
        }
    }
}
