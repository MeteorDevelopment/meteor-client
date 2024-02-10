package me.jellysquid.mods.lithium.common.util.tuples;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.poi.PointOfInterest;

public record SortedPointOfInterest(PointOfInterest poi, double distanceSq) {

    public SortedPointOfInterest(PointOfInterest poi, BlockPos origin) {
        this(poi, poi.getPos().getSquaredDistance(origin));
    }

    public BlockPos getPos() {
        return this.poi.getPos();
    }

    public int getX() {
        return this.getPos().getX();
    }

    public int getY() {
        return this.getPos().getY();
    }

    public int getZ() {
        return this.getPos().getZ();
    }
}
