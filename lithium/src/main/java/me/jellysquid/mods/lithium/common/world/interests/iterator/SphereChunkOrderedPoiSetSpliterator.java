package me.jellysquid.mods.lithium.common.world.interests.iterator;

import me.jellysquid.mods.lithium.common.util.Distances;
import me.jellysquid.mods.lithium.common.world.interests.RegionBasedStorageSectionExtended;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.poi.PointOfInterestSet;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class SphereChunkOrderedPoiSetSpliterator extends Spliterators.AbstractSpliterator<Stream<PointOfInterestSet>> {
    private final int limit;
    private final int minChunkZ;
    private final BlockPos origin;
    private final double radiusSq;
    private final RegionBasedStorageSectionExtended<PointOfInterestSet> storage;
    private final int maxChunkZ;
    int chunkX;
    int chunkZ;
    int iterated;

    public SphereChunkOrderedPoiSetSpliterator(int radius, BlockPos origin, RegionBasedStorageSectionExtended<PointOfInterestSet> storage) {
        super((long) ((origin.getX() + radius + 1 >> 4) - (origin.getX() - radius - 1 >> 4) + 1) * ((origin.getZ() + radius + 1 >> 4) - (origin.getZ() - radius - 1 >> 4) + 1), Spliterator.ORDERED);
        this.origin = origin;
        this.radiusSq = radius * radius;
        this.storage = storage;

        int minChunkX = origin.getX() - radius - 1 >> 4;
        int maxChunkX = origin.getX() + radius + 1 >> 4;
        this.minChunkZ = origin.getZ() - radius - 1 >> 4;
        this.maxChunkZ = origin.getZ() + radius + 1 >> 4;
        this.limit = (maxChunkX - minChunkX + 1) * ((this.maxChunkZ) - (this.minChunkZ) + 1);

        this.chunkX = minChunkX;
        this.chunkZ = this.minChunkZ;
        this.iterated = 0;
    }

    @Override
    public boolean tryAdvance(Consumer<? super Stream<PointOfInterestSet>> action) {
        while (true) {
            if (this.iterated >= this.limit) {
                return false;
            } else {
                this.iterated++;
                boolean progress = false;
                if (Distances.getMinChunkToBlockDistanceL2Sq(this.origin, this.chunkX, this.chunkZ) <= this.radiusSq) {
                    //future work: filter sections with too high distance on the y axis as well
                    action.accept(this.storage.getWithinChunkColumn(this.chunkX, this.chunkZ));
                    progress = true;
                }

                this.chunkZ++;
                if (this.chunkZ > maxChunkZ) {
                    this.chunkX++;
                    this.chunkZ = minChunkZ;
                }

                if (progress) {
                    return true;
                }
            }
        }
    }
}
