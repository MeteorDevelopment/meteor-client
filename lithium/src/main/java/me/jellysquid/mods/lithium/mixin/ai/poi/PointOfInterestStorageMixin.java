package me.jellysquid.mods.lithium.mixin.ai.poi;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Codec;
import me.jellysquid.mods.lithium.common.util.Distances;
import me.jellysquid.mods.lithium.common.world.interests.PointOfInterestSetExtended;
import me.jellysquid.mods.lithium.common.world.interests.PointOfInterestStorageExtended;
import me.jellysquid.mods.lithium.common.world.interests.RegionBasedStorageSectionExtended;
import me.jellysquid.mods.lithium.common.world.interests.iterator.NearbyPointOfInterestStream;
import me.jellysquid.mods.lithium.common.world.interests.iterator.SinglePointOfInterestTypeFilter;
import me.jellysquid.mods.lithium.common.world.interests.iterator.SphereChunkOrderedPoiSetSpliterator;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestSet;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.storage.SerializingRegionBasedStorage;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Mixin(PointOfInterestStorage.class)
public abstract class PointOfInterestStorageMixin extends SerializingRegionBasedStorage<PointOfInterestSet>
        implements PointOfInterestStorageExtended {

    public PointOfInterestStorageMixin(Path path, Function<Runnable, Codec<PointOfInterestSet>> codecFactory, Function<Runnable, PointOfInterestSet> factory, DataFixer dataFixer, DataFixTypes dataFixTypes, boolean dsync, DynamicRegistryManager dynamicRegistryManager, HeightLimitView world) {
        super(path, codecFactory, factory, dataFixer, dataFixTypes, dsync, dynamicRegistryManager, world);
    }

    /**
     * @reason Retrieve all points of interest in one operation
     * @author JellySquid
     */
    @Debug
    @SuppressWarnings("unchecked")
    @Overwrite
    public Stream<PointOfInterest> getInChunk(Predicate<RegistryEntry<PointOfInterestType>> predicate, ChunkPos pos,
                                              PointOfInterestStorage.OccupationStatus status) {
        return ((RegionBasedStorageSectionExtended<PointOfInterestSet>) this)
                .getWithinChunkColumn(pos.x, pos.z)
                .flatMap(set -> set.get(predicate, status));
    }

    /**
     * Gets a random POI that matches the requirements. Uses spherical radius.
     *
     * @reason Retrieve all points of interest in one operation, avoid stream code
     * @author JellySquid
     */
    @Overwrite
    public Optional<BlockPos> getPosition(Predicate<RegistryEntry<PointOfInterestType>> typePredicate, Predicate<BlockPos> posPredicate,
                                          PointOfInterestStorage.OccupationStatus status, BlockPos pos, int radius,
                                          Random rand) {
        ArrayList<PointOfInterest> list = this.withinSphereChunkSectionSorted(typePredicate, pos, radius, status);

        for (int i = list.size() - 1; i >= 0; i--) {
            //shuffle by swapping randomly
            PointOfInterest currentPOI = list.set(rand.nextInt(i + 1), list.get(i));
            list.set(i, currentPOI); //Move to the end of the unconsumed part of the list

            //consume while shuffling, abort shuffling when result found
            if (posPredicate.test(currentPOI.getPos())) {
                return Optional.of(currentPOI.getPos());
            }
        }

        return Optional.empty();
    }

    /**
     * Gets the closest POI that matches the requirements.
     *
     * @reason Avoid stream-heavy code, use a faster iterator and callback-based approach
     * @author 2No2Name
     */
    @Overwrite
    public Optional<BlockPos> getNearestPosition(Predicate<RegistryEntry<PointOfInterestType>> predicate, BlockPos pos, int radius,
                                                 PointOfInterestStorage.OccupationStatus status) {
        return this.getNearestPosition(predicate, null, pos, radius, status);
    }

    /**
     * Gets the closest POI that matches the requirements.
     * If there are several closest POIs, negative chunk coordinate first (sort by x, then z, then y)
     *
     * @reason Avoid stream-heavy code, use a faster iterator and callback-based approach
     * @author JellySquid, 2No2Name
     */
    @Overwrite
    public Optional<BlockPos> getNearestPosition(Predicate<RegistryEntry<PointOfInterestType>> predicate,
                                                 Predicate<BlockPos> posPredicate, BlockPos pos, int radius,
                                                 PointOfInterestStorage.OccupationStatus status) {
        Stream<PointOfInterest> pointOfInterestStream = this.streamOutwards(pos, radius, status, true, false, predicate, posPredicate == null ? null : poi -> posPredicate.test(poi.getPos()));
        return pointOfInterestStream.map(PointOfInterest::getPos).findFirst();
    }

    /**
     * Get number of matching POIs in sphere
     *
     * @reason Avoid stream-heavy code, use a faster iterator and callback-based approach
     * @author JellySquid
     */
    @Overwrite
    public long count(Predicate<RegistryEntry<PointOfInterestType>> predicate, BlockPos pos, int radius,
                      PointOfInterestStorage.OccupationStatus status) {
        return this.withinSphereChunkSectionSorted(predicate, pos, radius, status).size();
    }

    /**
     * Get all POI in sphere around origin with given radius. Order is vanilla order
     * Vanilla order (might be undefined, but pratically):
     * Chunk section order: Negative X first, if equal, negative Z first, if equal, negative Y first.
     * Within the chunk section: Whatever the internal order is (we are not modifying that)
     *
     * @author JellySquid
     * @reason Avoid stream-heavy code, use faster filtering and fetches
     */
    @Overwrite
    public Stream<PointOfInterest> getInCircle(Predicate<RegistryEntry<PointOfInterestType>> predicate, BlockPos sphereOrigin, int radius,
                                               PointOfInterestStorage.OccupationStatus status) {
        return this.withinSphereChunkSectionSortedStream(predicate, sphereOrigin, radius, status);
    }

    @Override
    public Optional<PointOfInterest> findNearestForPortalLogic(BlockPos origin, int radius, RegistryEntry<PointOfInterestType> type,
                                                               PointOfInterestStorage.OccupationStatus status,
                                                               Predicate<PointOfInterest> afterSortPredicate, WorldBorder worldBorder) {
        // Order of the POI:
        // return closest accepted POI (L2 distance). If several exist:
        // return the one with most negative Y. If several exist:
        // return the one with most negative X. If several exist:
        // return the one with most negative Z. If several exist: Be confused about two POIs being in the same location.

        boolean worldBorderIsFarAway = worldBorder == null || worldBorder.getDistanceInsideBorder(origin.getX(), origin.getZ()) > radius + 3;
        Predicate<PointOfInterest> poiPredicateAfterSorting;
        if (worldBorderIsFarAway) {
            poiPredicateAfterSorting = afterSortPredicate;
        } else {
            poiPredicateAfterSorting = poi -> worldBorder.contains(poi.getPos()) && afterSortPredicate.test(poi);
        }
        return this.streamOutwards(origin, radius, status, true, true, new SinglePointOfInterestTypeFilter(type), poiPredicateAfterSorting).findFirst();
    }

    private Stream<PointOfInterest> withinSphereChunkSectionSortedStream(Predicate<RegistryEntry<PointOfInterestType>> predicate, BlockPos origin,
                                                                         int radius, PointOfInterestStorage.OccupationStatus status) {
        double radiusSq = radius * radius;


        // noinspection unchecked
        RegionBasedStorageSectionExtended<PointOfInterestSet> storage = (RegionBasedStorageSectionExtended<PointOfInterestSet>) this;


        Stream<Stream<PointOfInterestSet>> stream = StreamSupport.stream(new SphereChunkOrderedPoiSetSpliterator(radius, origin, storage), false);

        return stream.flatMap((Stream<PointOfInterestSet> setStream) -> setStream.flatMap(
                (PointOfInterestSet set) -> set.get(predicate, status)
                        .filter(point -> Distances.isWithinCircleRadius(origin, radiusSq, point.getPos()))
        ));
    }

    private ArrayList<PointOfInterest> withinSphereChunkSectionSorted(Predicate<RegistryEntry<PointOfInterestType>> predicate, BlockPos origin,
                                                                      int radius, PointOfInterestStorage.OccupationStatus status) {
        double radiusSq = radius * radius;

        int minChunkX = origin.getX() - radius - 1 >> 4;
        int minChunkZ = origin.getZ() - radius - 1 >> 4;

        int maxChunkX = origin.getX() + radius + 1 >> 4;
        int maxChunkZ = origin.getZ() + radius + 1 >> 4;

        // noinspection unchecked
        RegionBasedStorageSectionExtended<PointOfInterestSet> storage = (RegionBasedStorageSectionExtended<PointOfInterestSet>) this;

        ArrayList<PointOfInterest> points = new ArrayList<>();
        Consumer<PointOfInterest> collector = point -> {
            if (Distances.isWithinCircleRadius(origin, radiusSq, point.getPos())) {
                points.add(point);
            }
        };

        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                for (PointOfInterestSet set : storage.getInChunkColumn(x, z)) {
                    ((PointOfInterestSetExtended) set).collectMatchingPoints(predicate, status, collector);
                }
            }
        }

        return points;
    }

    private Stream<PointOfInterest> streamOutwards(BlockPos origin, int radius,
                                                   PointOfInterestStorage.OccupationStatus status,
                                                   @SuppressWarnings("SameParameterValue") boolean useSquareDistanceLimit,
                                                   boolean preferNegativeY,
                                                   Predicate<RegistryEntry<PointOfInterestType>> typePredicate,
                                                   @Nullable Predicate<PointOfInterest> afterSortingPredicate) {
        // noinspection unchecked
        RegionBasedStorageSectionExtended<PointOfInterestSet> storage = (RegionBasedStorageSectionExtended<PointOfInterestSet>) this;

        return StreamSupport.stream(new NearbyPointOfInterestStream(typePredicate, status, useSquareDistanceLimit, preferNegativeY, afterSortingPredicate, origin, radius, storage), false);
    }

    @Shadow
    protected abstract void scanAndPopulate(ChunkSection section, ChunkSectionPos sectionPos, BiConsumer<BlockPos, PointOfInterestType> entryConsumer);
}
