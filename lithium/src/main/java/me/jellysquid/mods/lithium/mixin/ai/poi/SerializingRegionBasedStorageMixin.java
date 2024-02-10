package me.jellysquid.mods.lithium.mixin.ai.poi;

import com.google.common.collect.AbstractIterator;
import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.jellysquid.mods.lithium.common.util.Pos;
import me.jellysquid.mods.lithium.common.util.collections.ListeningLong2ObjectOpenHashMap;
import me.jellysquid.mods.lithium.common.world.interests.RegionBasedStorageSectionExtended;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.storage.SerializingRegionBasedStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType") // We don't get a choice, this is Minecraft's doing!
@Mixin(SerializingRegionBasedStorage.class)
public abstract class SerializingRegionBasedStorageMixin<R> implements RegionBasedStorageSectionExtended<R> {
    @Mutable
    @Shadow
    @Final
    private Long2ObjectMap<Optional<R>> loadedElements;

    @Shadow
    protected abstract Optional<R> get(long pos);

    @Shadow
    protected abstract void loadDataAt(ChunkPos pos);

    @Shadow
    @Final
    protected HeightLimitView world;
    private Long2ObjectOpenHashMap<BitSet> columns;

    @SuppressWarnings("rawtypes")
    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(Path path, Function codecFactory, Function factory, DataFixer dataFixer, DataFixTypes dataFixTypes, boolean dsync, DynamicRegistryManager dynamicRegistryManager, HeightLimitView world, CallbackInfo ci) {
        this.columns = new Long2ObjectOpenHashMap<>();
        this.loadedElements = new ListeningLong2ObjectOpenHashMap<>(this::onEntryAdded, this::onEntryRemoved);
    }

    private void onEntryRemoved(long key, Optional<R> value) {
        int y = Pos.SectionYIndex.fromSectionCoord(this.world, ChunkSectionPos.unpackY(key));

        // We only care about items belonging to a valid sub-chunk
        if (y < 0 || y >= Pos.SectionYIndex.getNumYSections(this.world)) {
            return;
        }

        int x = ChunkSectionPos.unpackX(key);
        int z = ChunkSectionPos.unpackZ(key);

        long pos = ChunkPos.toLong(x, z);
        BitSet flags = this.columns.get(pos);

        if (flags != null) {
            flags.clear(y);
            if (flags.isEmpty()) {
                this.columns.remove(pos);
            }
        }
    }

    private void onEntryAdded(long key, Optional<R> value) {
        int y = Pos.SectionYIndex.fromSectionCoord(this.world, ChunkSectionPos.unpackY(key));

        // We only care about items belonging to a valid sub-chunk
        if (y < 0 || y >= Pos.SectionYIndex.getNumYSections(this.world)) {
            return;
        }

        int x = ChunkSectionPos.unpackX(key);
        int z = ChunkSectionPos.unpackZ(key);

        long pos = ChunkPos.toLong(x, z);

        BitSet flags = this.columns.get(pos);

        if (flags == null) {
            this.columns.put(pos, flags = new BitSet(Pos.SectionYIndex.getNumYSections(this.world)));
        }

        flags.set(y, value.isPresent());
    }

    @Override
    public Stream<R> getWithinChunkColumn(int chunkX, int chunkZ) {
        BitSet sectionsWithPOI = this.getNonEmptyPOISections(chunkX, chunkZ);

        // No items are present in this column
        if (sectionsWithPOI.isEmpty()) {
            return Stream.empty();
        }

        List<R> list = new ArrayList<>();
        int minYSection = Pos.SectionYCoord.getMinYSection(this.world);
        for (int chunkYIndex = sectionsWithPOI.nextSetBit(0); chunkYIndex != -1; chunkYIndex = sectionsWithPOI.nextSetBit(chunkYIndex + 1)) {
            int chunkY = chunkYIndex + minYSection;
            //noinspection SimplifyOptionalCallChains
            R r = this.loadedElements.get(ChunkSectionPos.asLong(chunkX, chunkY, chunkZ)).orElse(null);
            if (r != null) {
                list.add(r);
            }
        }

        return list.stream();
    }

    @Override
    public Iterable<R> getInChunkColumn(int chunkX, int chunkZ) {
        BitSet sectionsWithPOI = this.getNonEmptyPOISections(chunkX, chunkZ);

        // No items are present in this column
        if (sectionsWithPOI.isEmpty()) {
            return Collections::emptyIterator;
        }

        Long2ObjectMap<Optional<R>> loadedElements = this.loadedElements;
        HeightLimitView world = this.world;

        return () -> new AbstractIterator<>() {
            private int nextBit = sectionsWithPOI.nextSetBit(0);


            @Override
            protected R computeNext() {
                // If the next bit is <0, that means that no remaining set bits exist
                while (this.nextBit >= 0) {
                    Optional<R> next = loadedElements.get(ChunkSectionPos.asLong(chunkX, Pos.SectionYCoord.fromSectionIndex(world, this.nextBit), chunkZ));

                    // Find and advance to the next set bit
                    this.nextBit = sectionsWithPOI.nextSetBit(this.nextBit + 1);

                    if (next.isPresent()) {
                        return next.get();
                    }
                }

                return this.endOfData();
            }
        };
    }

    private BitSet getNonEmptyPOISections(int chunkX, int chunkZ) {
        long pos = ChunkPos.toLong(chunkX, chunkZ);

        BitSet flags = this.getNonEmptySections(pos, false);

        if (flags != null) {
            return flags;
        }

        this.loadDataAt(new ChunkPos(pos));

        return this.getNonEmptySections(pos, true);
    }

    private BitSet getNonEmptySections(long pos, boolean required) {
        BitSet set = this.columns.get(pos);

        if (set == null && required) {
            throw new NullPointerException("No data is present for column: " + new ChunkPos(pos));
        }

        return set;
    }
}
