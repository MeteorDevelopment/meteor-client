package me.jellysquid.mods.lithium.common.entity.block_tracking;

import it.unimi.dsi.fastutil.objects.Reference2DoubleArrayMap;
import me.jellysquid.mods.lithium.common.block.BlockStateFlags;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.Box;

public final class BlockCache {
    // To avoid slowing down setblock operations, only start caching after 3 Seconds = 60 gameticks with N accesses per tick
    private static final int MIN_DELAY = 60 * 5;
    private int initDelay; //Changing MIN_DELAY should not affect correctness, just performance in some cases

    private Box trackedPos;
    private SectionedBlockChangeTracker tracker;
    private long trackingSince;

    private boolean canSkipSupportingBlockSearch;

    private boolean canSkipBlockTouching;
    //0 if not touching fire/lava. 1 if touching fire/lava. -1 if not cached
    private byte cachedTouchingFireLava;
    //0 if not suffocating. 1 if touching suffocating. -1 if not cached
    private byte cachedIsSuffocating;
    //Touched fluid's height IF fluid pushing is 0. Touched fluid height is 0 when not touching that fluid. Not in collection: No cached value (uninitialized OR fluid pushing is not 0)
    private final Reference2DoubleArrayMap<TagKey<Fluid>> fluidType2FluidHeightMap;

    //Future: maybe cache last failed movement vector

    public BlockCache() {
        this.tracker = null;
        this.trackedPos = null;
        this.initDelay = 0;
        this.fluidType2FluidHeightMap = new Reference2DoubleArrayMap<>(2);
    }

    public boolean isTracking() {
        return this.tracker != null;
    }

    public void initTracking(Entity entity) {
        if (this.isTracking()) {
            throw new IllegalStateException("Cannot init cache that is already initialized!");
        }
        this.tracker = SectionedBlockChangeTracker.registerAt(entity.getWorld(), entity.getBoundingBox(), BlockStateFlags.ANY);
        this.initDelay = 0;
        this.resetCachedInfo();
    }
    public void updateCache(Entity entity) {
        if (this.isTracking() || this.initDelay >= MIN_DELAY) {
            Box boundingBox = entity.getBoundingBox();
            if (boundingBox.equals(this.trackedPos)) {
                if (!this.isTracking()) {
                    this.initTracking(entity);
                } else if (!this.tracker.isUnchangedSince(this.trackingSince)) {
                    this.resetCachedInfo();
                }
            } else {
                if (this.isTracking() && !this.tracker.matchesMovedBox(boundingBox)) {
                    this.tracker.unregister();
                    this.tracker = null;
                }
                this.resetTrackedPos(boundingBox);
            }
        } else {
            this.initDelay++;
        }
    }

    public void resetTrackedPos(Box boundingBox) {
        this.trackedPos = boundingBox;
        this.initDelay = 0;
        this.resetCachedInfo();
    }

    public void resetCachedInfo() {
        this.trackingSince = !this.isTracking() ? Long.MIN_VALUE : this.tracker.getWorldTime();
        this.canSkipSupportingBlockSearch = false;
        this.cachedIsSuffocating = (byte) -1;
        this.cachedTouchingFireLava = (byte) -1;
        this.canSkipBlockTouching = false;
        this.fluidType2FluidHeightMap.clear();
    }

    public void remove() {
        if (this.tracker != null) {
            this.tracker.unregister();
        }
    }

    public boolean canSkipBlockTouching() {
        return this.isTracking() && this.canSkipBlockTouching;
    }

    public void setCanSkipBlockTouching(boolean value) {
        this.canSkipBlockTouching = value;
    }

    public double getStationaryFluidHeightOrDefault(TagKey<Fluid> fluid, double defaultValue) {
        if (this.isTracking()) {
            return this.fluidType2FluidHeightMap.getOrDefault(fluid, defaultValue);
        }
        return defaultValue;
    }

    public void setCachedFluidHeight(TagKey<Fluid> fluid, double fluidHeight) {
        this.fluidType2FluidHeightMap.put(fluid, fluidHeight);
    }

    public byte getIsTouchingFireLava() {
        if (this.isTracking()) {
            return this.cachedTouchingFireLava;
        }
        return (byte) -1;
    }

    public void setCachedTouchingFireLava(boolean b) {
        this.cachedTouchingFireLava = b ? (byte) 1 : (byte) 0;
    }

    public byte getIsSuffocating() {
        if (this.isTracking()) {
            return this.cachedIsSuffocating;
        }
        return (byte) -1;
    }

    public void setCachedIsSuffocating(boolean b) {
        this.cachedIsSuffocating = b ? (byte) 1 : (byte) 0;
    }

    public boolean canSkipSupportingBlockSearch() {
        return this.isTracking() && this.canSkipSupportingBlockSearch;
    }

    public void setCanSkipSupportingBlockSearch(boolean canSkip) {
        this.canSkipSupportingBlockSearch = canSkip;
    }
}
