package me.jellysquid.mods.lithium.mixin.ai.poi.fast_portals;

import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.WorldView;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.poi.PointOfInterestSet;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.storage.SerializingRegionBasedStorage;
import org.spongepowered.asm.mixin.*;

import java.nio.file.Path;
import java.util.Optional;

@Mixin(PointOfInterestStorage.class)
public abstract class PointOfInterestStorageMixin extends SerializingRegionBasedStorage<PointOfInterestSet> {

    @Shadow
    @Final
    private LongSet preloadedChunks;

    @Unique
    private final LongSet preloadedCenterChunks = new LongOpenHashSet();
    @Unique
    private int preloadRadius = 0;

    public PointOfInterestStorageMixin(
            Path path, DataFixer dataFixer, boolean dsync,
            DynamicRegistryManager registryManager, HeightLimitView world
    ) {
        super(
                path, PointOfInterestSet::createCodec, PointOfInterestSet::new,
                dataFixer, DataFixTypes.POI_CHUNK, dsync, registryManager, world
        );
    }

    /**
     * @author Crec0, 2No2Name
     * @reason Streams in this method cause unnecessary lag. Simply rewriting this to not use streams, we gain
     * considerable performance. Noticeable when large amount of entities are traveling through nether portals.
     * Furthermore, caching whether all surrounding chunks are loaded is more efficient than caching the state
     * of single chunks only.
     */
    @Overwrite
    public void preloadChunks(WorldView worldView, BlockPos pos, int radius) {
        if (this.preloadRadius != radius) {
            //Usually there is only one preload radius per PointOfInterestStorage. Just in case another mod adjusts it dynamically, we avoid
            //assuming its value.
            this.preloadedCenterChunks.clear();
            this.preloadRadius = radius;
        }
        long chunkPos = ChunkPos.toLong(pos);
        if (this.preloadedCenterChunks.contains(chunkPos)) {
            return;
        }
        int chunkX = ChunkSectionPos.getSectionCoord(pos.getX());
        int chunkZ = ChunkSectionPos.getSectionCoord(pos.getZ());

        int chunkRadius = Math.floorDiv(radius, 16);
        int maxHeight = this.world.getTopSectionCoord() - 1;
        int minHeight = this.world.getBottomSectionCoord();

        for (int x = chunkX - chunkRadius, xMax = chunkX + chunkRadius; x <= xMax; x++) {
            for (int z = chunkZ - chunkRadius, zMax = chunkZ + chunkRadius; z <= zMax; z++) {
                lithium$preloadChunkIfAnySubChunkContainsPOI(worldView, x, z, minHeight, maxHeight);
            }
        }
        this.preloadedCenterChunks.add(chunkPos);
    }

    @Unique
    private void lithium$preloadChunkIfAnySubChunkContainsPOI(WorldView worldView, int x, int z, int minSubChunk, int maxSubChunk) {
        ChunkPos chunkPos = new ChunkPos(x, z);
        long longChunkPos = chunkPos.toLong();

        if (this.preloadedChunks.contains(longChunkPos)) return;

        for (int y = minSubChunk; y <= maxSubChunk; y++) {
            Optional<PointOfInterestSet> section = this.get(ChunkSectionPos.asLong(x, y, z));
            if (section.isPresent()) {
                boolean result = section.get().isValid();
                if (result) {
                    if (this.preloadedChunks.add(longChunkPos)) {
                        worldView.getChunk(x, z, ChunkStatus.EMPTY);
                    }
                    break;
                }
            }
        }
    }
}
