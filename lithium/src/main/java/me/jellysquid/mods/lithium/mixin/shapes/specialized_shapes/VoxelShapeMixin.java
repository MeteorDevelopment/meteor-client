package me.jellysquid.mods.lithium.mixin.shapes.specialized_shapes;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.util.math.AxisCycleDirection;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelSet;
import net.minecraft.util.shape.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Implement faster methods for determining penetration during collision resolution.
 */
@Mixin(VoxelShape.class)
public abstract class VoxelShapeMixin {
    private static final double POSITIVE_EPSILON = +1.0E-7D;
    private static final double NEGATIVE_EPSILON = -1.0E-7D;

    @Shadow
    @Final
    public VoxelSet voxels;

    @Shadow
    public abstract boolean isEmpty();

    @Shadow
    protected abstract double getPointPosition(Direction.Axis axis, int index);

    @Shadow
    public abstract DoubleList getPointPositions(Direction.Axis axis);

    /**
     * @reason Use optimized implementation which delays searching for coordinates as long as possible
     * @author JellySquid
     */
    @Overwrite
    public double calculateMaxDistance(AxisCycleDirection cycleDirection, Box box, double maxDist) {
        if (this.isEmpty()) {
            return maxDist;
        }

        if (Math.abs(maxDist) < POSITIVE_EPSILON) {
            return 0.0D;
        }

        AxisCycleDirection cycle = cycleDirection.opposite();

        Direction.Axis axisX = cycle.cycle(Direction.Axis.X);
        Direction.Axis axisY = cycle.cycle(Direction.Axis.Y);
        Direction.Axis axisZ = cycle.cycle(Direction.Axis.Z);

        int minY = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int minZ = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        int x, y, z;

        double dist;

        if (maxDist > 0.0D) {
            double max = box.getMax(axisX);
            int maxIdx = this.getCoordIndex(axisX, max - POSITIVE_EPSILON);

            int maxX = this.voxels.getSize(axisX);

            for (x = maxIdx + 1; x < maxX; ++x) {
                minY = minY == Integer.MIN_VALUE ? Math.max(0, this.getCoordIndex(axisY, box.getMin(axisY) + POSITIVE_EPSILON)) : minY;
                maxY = maxY == Integer.MIN_VALUE ? Math.min(this.voxels.getSize(axisY), this.getCoordIndex(axisY, box.getMax(axisY) - POSITIVE_EPSILON) + 1) : maxY;

                for (y = minY; y < maxY; ++y) {
                    minZ = minZ == Integer.MIN_VALUE ? Math.max(0, this.getCoordIndex(axisZ, box.getMin(axisZ) + POSITIVE_EPSILON)) : minZ;
                    maxZ = maxZ == Integer.MIN_VALUE ? Math.min(this.voxels.getSize(axisZ), this.getCoordIndex(axisZ, box.getMax(axisZ) - POSITIVE_EPSILON) + 1) : maxZ;

                    for (z = minZ; z < maxZ; ++z) {
                        if (this.voxels.inBoundsAndContains(cycle, x, y, z)) {
                            dist = this.getPointPosition(axisX, x) - max;

                            if (dist >= NEGATIVE_EPSILON) {
                                maxDist = Math.min(maxDist, dist);
                            }

                            return maxDist;
                        }
                    }
                }
            }
        } else if (maxDist < 0.0D) {
            double min = box.getMin(axisX);
            int minIdx = this.getCoordIndex(axisX, min + POSITIVE_EPSILON);

            for (x = minIdx - 1; x >= 0; --x) {
                minY = minY == Integer.MIN_VALUE ? Math.max(0, this.getCoordIndex(axisY, box.getMin(axisY) + POSITIVE_EPSILON)) : minY;
                maxY = maxY == Integer.MIN_VALUE ? Math.min(this.voxels.getSize(axisY), this.getCoordIndex(axisY, box.getMax(axisY) - POSITIVE_EPSILON) + 1) : maxY;

                for (y = minY; y < maxY; ++y) {
                    minZ = minZ == Integer.MIN_VALUE ? Math.max(0, this.getCoordIndex(axisZ, box.getMin(axisZ) + POSITIVE_EPSILON)) : minZ;
                    maxZ = maxZ == Integer.MIN_VALUE ? Math.min(this.voxels.getSize(axisZ), this.getCoordIndex(axisZ, box.getMax(axisZ) - POSITIVE_EPSILON) + 1) : maxZ;

                    for (z = minZ; z < maxZ; ++z) {
                        if (this.voxels.inBoundsAndContains(cycle, x, y, z)) {
                            dist = this.getPointPosition(axisX, x + 1) - min;

                            if (dist <= POSITIVE_EPSILON) {
                                maxDist = Math.max(maxDist, dist);
                            }

                            return maxDist;
                        }
                    }
                }
            }
        }

        return maxDist;
    }

    /**
     * Inlines the lambda passed to MathHelper#binarySearch. Simplifies the implementation very slightly for additional
     * speed.
     *
     * @reason Use faster implementation
     * @author JellySquid
     */
    @Overwrite
    public int getCoordIndex(Direction.Axis axis, double coord) {
        DoubleList list = this.getPointPositions(axis);

        int size = this.voxels.getSize(axis);

        int start = 0;
        int end = size + 1 - start;

        while (end > 0) {
            int middle = end / 2;
            int idx = start + middle;

            if (idx >= 0 && (idx > size || coord < list.getDouble(idx))) {
                end = middle;
            } else {
                start = idx + 1;
                end -= middle + 1;
            }
        }

        return start - 1;
    }
}
