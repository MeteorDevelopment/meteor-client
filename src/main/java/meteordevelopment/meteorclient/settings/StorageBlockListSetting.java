/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.ObjectIterators;
import meteordevelopment.meteorclient.utils.misc.MeteorIdentifier;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryEntryList;
import net.minecraft.util.registry.RegistryKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class StorageBlockListSetting extends Setting<List<BlockEntityType<?>>> {
    public static final BlockEntityType<?>[] STORAGE_BLOCKS = { BlockEntityType.FURNACE, BlockEntityType.CHEST, BlockEntityType.TRAPPED_CHEST, BlockEntityType.ENDER_CHEST, BlockEntityType.DISPENSER, BlockEntityType.DROPPER, BlockEntityType.HOPPER, BlockEntityType.SHULKER_BOX, BlockEntityType.BARREL, BlockEntityType.SMOKER, BlockEntityType.BLAST_FURNACE, BlockEntityType.CAMPFIRE };

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
    public NbtCompound save(NbtCompound tag) {
        NbtList valueTag = new NbtList();
        for (BlockEntityType<?> type : get()) {
            Identifier id = Registry.BLOCK_ENTITY_TYPE.getId(type);
            if (id != null) valueTag.add(NbtString.of(id.toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public List<BlockEntityType<?>> load(NbtCompound tag) {
        get().clear();

        NbtList valueTag = tag.getList("value", 8);
        for (NbtElement tagI : valueTag) {
            BlockEntityType<?> type = Registry.BLOCK_ENTITY_TYPE.get(new Identifier(tagI.asString()));
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

    private static class SRegistry extends Registry<BlockEntityType<?>> {
        public SRegistry() {
            super(RegistryKey.ofRegistry(new MeteorIdentifier("storage-blocks")), Lifecycle.stable());
        }

        @Override
        public DataResult<RegistryEntry<BlockEntityType<?>>> getOrCreateEntryDataResult(RegistryKey<BlockEntityType<?>> key) {
            return null;
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
        public Lifecycle getEntryLifecycle(BlockEntityType<?> object) {
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
        public boolean contains(RegistryKey<BlockEntityType<?>> key) {
            return false;
        }

        @Override
        public Set<Map.Entry<RegistryKey<BlockEntityType<?>>, BlockEntityType<?>>> getEntrySet() {
            return null;
        }

        @Override
        public Optional<RegistryEntry<BlockEntityType<?>>> getRandom(net.minecraft.util.math.random.Random random) {
            return Optional.empty();
        }

        @Override
        public Registry<BlockEntityType<?>> freeze() {
            return null;
        }

        @Override
        public RegistryEntry<BlockEntityType<?>> getOrCreateEntry(RegistryKey<BlockEntityType<?>> key) {
            return null;
        }

        @Override
        public RegistryEntry.Reference<BlockEntityType<?>> createEntry(BlockEntityType<?> value) {
            return null;
        }

        @Override
        public Optional<RegistryEntry<BlockEntityType<?>>> getEntry(int rawId) {
            return Optional.empty();
        }

        @Override
        public Optional<RegistryEntry<BlockEntityType<?>>> getEntry(RegistryKey<BlockEntityType<?>> key) {
            return Optional.empty();
        }

        @Override
        public Stream<RegistryEntry.Reference<BlockEntityType<?>>> streamEntries() {
            return null;
        }

        @Override
        public Optional<RegistryEntryList.Named<BlockEntityType<?>>> getEntryList(TagKey<BlockEntityType<?>> tag) {
            return Optional.empty();
        }

        @Override
        public RegistryEntryList.Named<BlockEntityType<?>> getOrCreateEntryList(TagKey<BlockEntityType<?>> tag) {
            return null;
        }

        @Override
        public Stream<Pair<TagKey<BlockEntityType<?>>, RegistryEntryList.Named<BlockEntityType<?>>>> streamTagsAndEntries() {
            return null;
        }

        @Override
        public Stream<TagKey<BlockEntityType<?>>> streamTags() {
            return null;
        }

        @Override
        public boolean containsTag(TagKey<BlockEntityType<?>> tag) {
            return false;
        }

        @Override
        public void clearTags() {

        }

        @Override
        public void populateTags(Map<TagKey<BlockEntityType<?>>, List<RegistryEntry<BlockEntityType<?>>>> tagEntries) {

        }

        @Override
        public Set<RegistryKey<BlockEntityType<?>>> getKeys() {
            return null;
        }
    }
}
